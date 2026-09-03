# SkyGit

A simple Git CLI tool for analysing, mirroring, and serving Git repositories.

## Features

* Repository statistics and commit analysis
* Contributor statistics
* Repository mirroring
* Local Git server

## Command Reference

| Command                                    | Description                   |
| ------------------------------------------ | ----------------------------- |
| `skygit <repository>`                      | Display repository statistics |
| `skygit mirror <repository> <destination>` | Mirror a repository           |
| `skygit server <baseDir> [port]`           | Start a local Git server      |


## Project Status

SkyGit is currently an experimental project and is being developed as a way to explore Scala while building a useful Git utility.

Planned features include additional repository statistics, terminal visualisations, and report formats.
