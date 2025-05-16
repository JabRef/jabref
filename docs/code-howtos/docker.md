# Docker images offered by JabRef

## JabKit

`Dockerfile.jabkit`

Building locally:

    docker build -f Dockerfile.jabkit -t jabkit:latest .

Running locally:

     docker run --rm -it jabkit:latest --help

## JabSrv

`Dockerfile.jabsrv`

Building locally:

    docker build -f Dockerfile.jabsrv -t jabkit:latest .

Running locally:

    docker run --rm -it -p 6050:6050 jabsrv:latest -h 0.0.0.0
