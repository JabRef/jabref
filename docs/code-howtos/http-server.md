---
parent: Code Howtos
---
# HTTP Server

## Get SSL Working

(Based on <https://stackoverflow.com/a/57511038/873282>)

Howto for Windows - other operating systems work similar:

1. As admin `choco install mkcert`
2. As admin: `mkcert -install`
3. `cd %APPDATA%\..\local\org.jabref\jabref\ssl`
4. `mkcert -pkcs12 jabref.desktop jabref localhost 127.0.0.1 ::1`
5. Rename the file to `server.p12`

Note: If you do not do this, you get following error message:

    Could not find server key store C:\Users\USERNAME\AppData\Local\org.jabref\jabref\ssl\server.p12.
