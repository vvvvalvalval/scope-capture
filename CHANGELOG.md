# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Added
- Basic example-based tests for spy and brk
### Fixed 
- default CS logger prints directly to standard output so as not to get mixed with emitted JavaScript.

## 0.1.2 - 2017-10-18
## Changed
- Removed core.async, used clojure.core/promise for BRK

## 0.1.1 - 2017-10-17
### Fixed
- Fixed throwing of error when Code Site not found.

## 0.1.0 - 2017-10-02
### Added
- API for saving, accessing and recreating scope + suspending / resuming execution in `sc.api`
- clojure.main repl in `sc.repl`
- Docstrings
- Tutorial

[Unreleased]: https://github.com/vvvvalvalval/scope-capture/compare/0.1.2...HEAD
[0.1.2]: https://github.com/vvvvalvalval/scope-capture/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/vvvvalvalval/scope-capture/compare/0.1.0...0.1.1
