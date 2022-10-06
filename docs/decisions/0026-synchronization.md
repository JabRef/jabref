---
nav_order: 26
parent: Decision Records
---

# Synchronization with remote databases

## Context and Problem Statement

Synchronize the data in a library to a remote database, while handling conflicts and supporting offline-first paradigm.  

## Decision Drivers

See https://github.com/JabRef/jabref/issues/7618

## Considered Options

See https://github.com/JabRef/jabref/issues/7618

## Decision Outcome

The following algorithm is highly inspired by the replication protocols of [CouchDB](https://docs.couchdb.org/en/stable/replication/protocol.html) and [RxDB](https://rxdb.info/replication.html).

From a high-level perspective, the sync algorithm is very similar with git: both the server and the client have their own change histories, and the client has to first pull and merge changes from the server before pushing its new state to the server.
Thus, it is divided into three phases:

1. `Pull phase`: The server sends its local changes to the client.
2. `Merge phase`: The client and server merge their local changes.
3. `Push phase`: The client sends its local changes to the server.

In order to support this, additional metadata is kept for each entry:
- `ID`: An unique identifier for the entry (will be a UUID).
- `Revision`: A [MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control) token value that corresponds to a version of the entry saved in the server. It has the pattern: `N-hash` where `N` is an increasing positive integer and `hash` is the hash of the entry (i.e., of all the data except for the `Revision`).

At this point, we could already sync the server and client by asking the server for all up-to-date entries and then using the `Revision` information to merge with the local data. However, this is highly inefficient as the whole database has to be send over the wire. A small improvement is gained by first asking only for tuples of `ID` and `Revision`, and only pull the complete entry if the local data is outdated or in conflict.
However, this still requires to send quite a bit of data. Instead we will use the following refinement.

### Sync Algorithm

#### Pulling

The clients asks the server for a list of documents that changed since the last checkpoint. (Creating a checkpoint is  explained further below.) 
The server responses with a list of these entries together with their `Revision` information.

#### Merging

The client merges the changes from the server into its local database.
The data is merged on a per-entries case.
Based on the `Revision` of server and client, the following cases can occur:
- The server's `Revision` is higher than the client's `Revision`: If the client's entry is dirty, then the user is shown a message to resolve the conflict (see conflict handling below); otherwise the client's entry is replaced by the server's one (including the revision). 
- The server's `Revision` is equal to the client's `Revision`: Both entries are up-to-date and nothing has to be done. This case may happen if the library is synchronized by other means.
- The server's `Revision` is lower than the client's `Revision`: This should never be the case, as revisions are only increased on the server. Show error message to user.

*Conflict handling*: If the user simply overwrites the local entry with the remote entry, then its `Revision` is updated as well and it is no longer marked as dirty. Otherwise, its `Revision` is updated to the one provided by the server, but it is still marked as dirty.

#### Pushing

The client sends the following information back to the client:
  ?? - `LastSync`: The server's last .??
   - `Entries`: The entries marked dirty (along with their `Revision` data).

The client sends all its changes that are not yet in the server to the server.
The server merges the changes into its local database.
As the client already merged the server's changes, the server's `Revision` is always higher than the client's `Revision`.
Thus, the server always updates its local data with the client's data.

#### Checkpoints

Checkpoints are used to keep track of the last synced state of the client and the server.
In order to support offline-first, the client has to be able to merge changes that it made while being offline.
To support this, the client has to store a checkpoint `Prev` in its local database.
This checkpoint is a tuple of `ID` and `Revision` and is updated after every merge.
The checkpoint is then used as the `Prev` parameter in the `Pull phase`.
As the client has to be able to merge its local changes even if the server is down, the client can not use the `Cur` checkpoint from the server.
Instead, the client has to store its own `Cur` checkpoint.

### Why?

#### Why do we need an ID? Is the BibTeX key not enough?
The ID needs to be unique at the very least across the library and should stay constant in time. Both features cannot be ensured for BibTeX keys. 

#### Why do we need `Revisions`? Are `updatedAt` timeflags not enough?
Yes


f you want to update or delete a document, CouchDB expects you to include the _rev field of the revision you wish to change. When CouchDB accepts the change, it will generate a new revision number. 


It is important to note that replication replicates the database only as it was at the point in time when replication was started. So, any additions, modifications, or deletions subsequent to the start of replication will not be replicated.




The client keeps track of the changes that happened in the library since the last time the client was synchronized with the server. Each change is a JSON object with the same fields as the server.
