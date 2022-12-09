Tesla Split Local Repository
============================

This project is being archived, while main features are today provided by Maven Resolver 1.8+, see here https://maven.apache.org/resolver/local-repository.html#split-local-repository

An extension for Tesla that makes it use an alternative format for the local repository where snapshots and releases
are separated into distinct base directories. In more detail, the local repository is organized as follows:

    ${maven.repo.local}/
    +- ls/     (base directory for snapshots that have been locally built/installed)
       +- ...  (well-known Maven layout)
    +- lr/     (base directory for releases that have been locally built/installed)
       +- ...  (well-known Maven layout)
    +- rs/     (base directory for snapshots that have been remotely downloaded)
       +- ...  (well-known Maven layout)
    +- rr/     (base directory for releases that have been remotely downloaded)
       +- ...  (well-known Maven layout)

Obviously, this repository layout is not compatible with the traditional local repository layout, i.e. it makes no sense
to share such a local repository with Maven 2.x or even stock Maven 3.x. The primary purpose of this layout is to
simplify periodic cleanup of snapshots during continuous integration builds.

**Warning:** Some Maven plugins bypass the repository API and directly manipulate files within the local repository,
assuming the traditional layout. Apparently, builds using such plugins will not function properly when this extension
is used.
