---
nav_order: 26
parent: Decision Records
---

# Synchronization with remote databases

## Context and Problem Statement

Synchronize the data in a library to a remote database, while handling conflicts and supporting offline-first paradigm.

## Decision Drivers

- Updates from the remote should be pulled in
- No updates should get lost
- Easy to implement
- Easy to maintain

## Considered Options

- Self-made algorithm
- Algorithm based on "optimistic offline lock"

## Decision Outcome

Chosen option: "Self-made algorithm", because simplest to implemement

## Pros and Cons of the Options

### Algorithm based on "optimistic offline lock"

[Optimistic Offline Lock](https://martinfowler.com/eaaCatalog/optimisticOfflineLock.html) is a well-established technique to prevent conflicts in concurrent business transactions.
It assumes that the chance of conflict is low.
Implementation details are found at <https://www.baeldung.com/cs/offline-concurrency-control>.

This algorithm is already in place since 2016 for JabRef synchronizing with a PostgreSQL backend and a MySQL backend.

### Self-made algorithm

The following algorithm is highly inspired by the replication protocols of [CouchDB](https://docs.couchdb.org/en/stable/replication/protocol.html) and [RxDB](https://rxdb.info/replication.html).

From a high-level perspective, the sync algorithm is very similar with git: both the server and the client have their own change histories, and the client has to first pull and merge changes from the server before pushing its new state to the server.
The sync process is incremental and only examines entries updated since the last sync.
Each sync cycle is divided into three phases:

1. `Pull phase`: The server sends its local changes to the client.
2. `Merge phase`: The client and server merge their local changes.
3. `Push phase`: The client sends its local changes to the server.

In order to support this, additional metadata is kept for each item:

- `ID`: An unique identifier for the entry (will be a UUID).
- `Revision`: A [MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control) token value that corresponds to a version of the item saved in the server. It has the pattern: `N-hash` where the generation ID `N` is an increasing positive integer and `hash` is the hash of the item (i.e., of all the data except for the `Revision`).

From now on, we will only discuss the sync of entries, but the sync of other data such as the groups tree will work similarily.

At this point, we could already sync the server and client by asking the server for all up-to-date entries and then using the `Revision` information to merge with the local data. However, this is highly inefficient as the whole database has to be send over the wire. A small improvement is gained by first asking only for tuples of `ID` and `Revision`, and only pull the complete entry if the local data is outdated or in conflict.
However, this still requires to send quite a bit of data. Instead we will use the following refinement.

#### Sync Algorithm

##### Pulling

The clients asks the server for a list of documents that changed since the last checkpoint. (Creating a checkpoint is explained further below.) 
The server responses with a batched list of these entries together with their `Revision` information. These entries could also be tombstones. Each batch includes also a checkpoint `To` that has the meaning "all changes to this point in time are included in the current batch.

Once the pull doesn't give any further changes, the client switches to a event-based strategy and observes new changes by subscribing to the event bus provided by the server. (This is more an implementation detail than a conceptual difference.)

##### Merging

The client merges the changes from the server into its local database.
The data is merged on a per-entry basis.
Based on the `Revision` of server and client, the following cases can occur:

- The server's `Revision` is higher than the client's `Revision`: If the client's entry is dirty, then the user is shown a message to resolve the conflict (see conflict handling below); otherwise the client's entry is replaced by the server's one (including the revision). 
- The server's `Revision` is equal to the client's `Revision`: Both entries are up-to-date and nothing has to be done. This case may happen if the library is synchronized by other means.
- The server's `Revision` is lower than the client's `Revision`: This should never be the case, as revisions are only increased on the server. Show error message to user.

If the entry returned by the server is a tombstone, then:

- If the client's entry is also a tombstone, then we don't have to do anything.
- If the client's entry is dirty, then the user is shown a message to resolve the conflict (see conflict handling below);
- Otherwise the client's entry is deleted. There is no need to keep track of this as a local tombstone. 

*Conflict handling*: If the user chooses to overwrite the local entry with the server entry, then the entry's `Revision` is updated as well and it is no longer marked as dirty. Otherwise, its `Revision` is updated to the one provided by the server, but it is still marked as dirty.

After the merging is done, the client sets its local checkpoint to the value of `To`.

##### Pushing

The client sends the following information back to the server:

- the list of entries that are marked dirty (along with their `Revision` data).
- the list of entries that are new, i.e., that don't have an `ID`.
- the list of tombstones, i.e., entries that have been deleted.

The server accepts only changes if the provided `Revision` coincides with the `Revision` stored on the server. If this is not the case, then the entry has been modified on the server since the last pull operation, and then the user needs to go through a new pull-merge-push cycle.

During the push operation, the user is not allowed to locally edit these entries that are currently pushed. After the push operation, all entries accepted by the server are marked clean. Moreover, the server will generate a new revision number for each accepted entry, which will then be stored locally. Entries rejected (as conflicts) by the server stay dirty and their `Revision` remains unchanged.

##### Start pull-merge-push cycle again

It is important to note that sync replicates the library only as it was at the point in time when the sync was started. So, any additions, modifications, or deletions on the server-side subsequent to the start of sync will not be replicated. For this reason, a new cycle is started.

##### Checkpoints

Checkpoints allow a sync task to be resumed from where it stopped, without having to start from the beginning.

The checkpoint locally stored by the client signals the time of the last server change that has been integrated into the local library.
Checkpoints are used to paginate the server-side changes.
In the implementation, the checkpoint will be a tuple consisting of the server time of the latest change and the highest ID of the entry in the batch; but its better to not depend on these semantics. 

The client has to store a checkpoint `LastSync` in its local database, and it is updated after every merge.
The checkpoint is then used as the `Since` parameter in the next Pull phase.

##### Dirty flags

Using dirty flags, the client keeps track of the changes that happened in the library since the last time the client was synchronized with the server. 

When JabRef loads a library into memory, it computes the hash for each entry and compares it with the hash in the entry's revision. In case of a difference between these hashes, the entry is marked dirty.
Moreover, an entry's dirty flag is set whenever it is modified by the user in JabRef.
The dirty flag is only cleared after a successful synchronization process.

There is no need to serialize the dirty flags on the client's side since they are recomputed upon loading.

##### Handling of deleted items

Deleted items are persisted as [tombstones](https://docs.couchbase.com/sync-gateway/current/managing-tombstones.html), which only contain the metadata `ID` and `Revision`.
Tombstones ensure that all synchronizing devices can identify that a previously existing entry has now been deleted.
On the client, a tombstone is created whenever an entry is deleted. Moreover, the client keeps a list of all entries in the library so that external deletions can be recognized when loading the library into memory. The local list of tombstones is cleared after it is sent to the server and the server acknowledged it.
On the server, tombstones are kept for a certain time span that is strictly larger then the time devices are allowed to not sign-in before removed as registered devices.

#### Scenarios

##### Sync stops after Pull

1. Client pulls changes since `T = 0`
2. Client starts with the merge but meanwhile closes JabRef without saving changes.
3. Client opens JabRef again.
4. Client pulls changes again from `T = 0` (since the checkpoint is still `T = 0`) and has to redo the conflict resolution.

This is the best we can do, since the user decided to not save its previous work.

However, consider the same steps but now in 2. the user decided to save its work. But the locally stored checkpoint is still `T = 0`, so that the user has to redo the conflict resolution again, with the difference that now the local version is the previously merge result.
*Future improvement:* We could send checkpoints for every entry and after each conflict resolution set the local checkpoint to the checkpoint of the entry.

##### Sync stops after Merge

1. Client pulls changes since `T = 0`
2. Client finishes the merge (this sets the checkpoint `T = 1̀`).
3. Client closes JabRef without saving changes (in particular, the checkpoint is not persisted as well).
3. Client opens JabRef again.
4. Client pulls changes again from `T = 0` (since the checkpoint is still `T = 0`) and has to redo the conflict resolution.

This is the best we can do, since the user decided to not save its previous work.

If the user decides in step 3 to save it changes, then in step 4 JabRef would pull changes starting from `T = 1` and the user doesn't have to redo the conflict resolution.

##### Sync after successful sync of client changes

1. Client modifies local data: `{id: 1, value: 0, _rev=1, _dirty=false} -> {id: 1, value: 1, _rev=1, _dirty=true}`.
2. Client pulls server changes. Suppose there are none.
3. Merge is thus not necessary. Sets checkpoint to `T = 1`.
4. Client pushes its changes to the server. Say this corresponds to `T = 2` on the server. On the server, this updates `{id: 1, value: 0, _rev=1, updatedAt=1} -> {id: 1, value: 1, _rev=2, updatedAt=2}` and on the client `{id: 1, value: 1, _rev=1, _dirty=true} -> {id: 1, value: 1, _rev=2, _dirty=false}`.
5. Client pulls changes starting from `T = 1` (the last local checkpoint). Server responds with `{id: 1, value: 1, _rev=2}, checkpoint={T: 2}`.
6. Client merges the 'changes', which in this case is trivial since the data on the server and client is the same.

This is suboptimal since the last pull response contains the full data of the entry although this data is already at the client.
*Possible future improvement:* First pull only the `IDs` and `Revisions` of the server-side changes, and then filter out the ones we already have locally before querying the complete entry. Downside is that this solution always needs one more request (per change batch) and its not clear if this outweighs the costs of sending the full entry. Altenatively, the server can remember where a change came from and then not send these changes back to that client. 

#### Reasons for some decisions

##### Why do we need an ID? Is the BibTeX key not enough?

The ID needs to be unique at the very least across the library and should stay constant in time. Both features cannot be ensured for BibTeX keys. 

##### Why do we need `Revisions`? Are `updatedAt` timeflags not enough?

A revision has two parts:

- the generation ID: This is essentially a clock local to the entry that ticks whenever the entry is synced with the server. Since for us there is only one server, strictly speaking, it would suffice to use the global server time for this. Moreover, for the sync algorithm, the client would only need to store the revision/server time during the pull-push cycle (to make sure that during this time the entry is not modified again on the server). However, the generation ID is only a tiny data blob and it gives a bit of additional security/consistency during the merge operation, so we keep it around all the time.
- the hash: The hash is only used on the client to determine whether an entry has been changed outside of JabRef, so this is needed in either case.

##### Why don't we need to keep the whole revision history as its done in CouchDB?

The revision history is used by CouchDB to find a common ancestor of two given revisions. This is needed since CouchDB provides main-main sync. However, in our setting, we have a central server and thus the last synced revision is *the* common ancestor for both the new server and client revision. See the question below for an example were the local revision history might be helpful. 

##### Why is a dirty flag enough on the client? Why don't we need local revisions?

In CouchDB every client has their own history of revisions. This is needed to have a deterministic conflict resolution that can run on both the server and client side independently. In this setting, it is important to determine which revision is older, which is then declared to be the winner. However, we don't need an automatic conflict resolution: whenever there is a conflict, the user is asked to resolve it. For this it is not important to know how many times (and when) the user changed the entry locally. It suffices to know that it changed at some point from the last synced version.

Local revision histories could be helpful in scenarios such as the following:

1. Device A is offline, and the user changes an entry.
2. The user sends this changed entry to Device B (say, via git).
3. The user further modifies the entry on Device B.
4. The user syncs Device B with the server.
5. The user syncs Device A with the server.

Without local revisions, it is not possible for Device A to figure out that the entry from the server logically evolved from its own local version. Instead, it shows a conflict message since the entry changed locally (step 1) and there is a newer revision on the server (from step 4).

## More Information 

- [CouchDB style sync and conflict resolution on Postgres with Hasura](https://hasura.io/blog/couchdb-style-conflict-resolution-rxdb-hasura/): Explains how to implement a sync algorithm in the style of CouchDB on your own
- [A Comparison of Offline Sync Protocols and Implementations](https://offlinefirst.org/sync/)
- [Offline data synchronization](https://developer.ibm.com/articles/offline-data-synchronization-strategies/): Discusses different strategies for offline data sync, and when to use which.
- Transaction Processing: Concepts and Techniques
- Transactional Information Systems: Theory, Algorithms, and the Practice of Concurrency Control and Recovery 
