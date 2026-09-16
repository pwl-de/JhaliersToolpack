# JhaliersToolpack

A malicious Minecraft utility plugin for testing, experimentation and server-side research.

## Features

* Custom `!` chat commands
* `#<command>` console command execution
* Live player/console command logging
* Persistent whitelist / automatic unban
* Restricted server shell
* File browsing and file operations
* Plugin JAR duplication
* Player information inspection
* Server performance monitoring
* Player freeze / unfreeze
* Ping monitoring

## Main Commands

```text
!help
!ls [directory]
!cat <file>
!inf <name.jar>
!wlsustain
!loglisten
!shell
#<command>
```

## Developer Commands

```text
/dev help
/dev playerinfo [player]
/dev perf
/dev ping [player]
/dev pingall
/dev freeze <player>
/dev unfreeze <player>
```

## Access

Toolpack chat commands are restricted to configured players. - There simplay edit the source code and recompile the .jar File.