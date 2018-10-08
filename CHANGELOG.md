# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## 0.3.2 - 2018-10-02
### Fixed
- Wrapped Throwable in validate-ep-identifier with reader conditional

## 0.3.1 - 2018-07-18
### Fixed
- Missing require in impl.cljc
- Fixed error thrown by last-ep-id when no EP saved yet

## 0.3.0 - 2018-07-17
### Added
- spyqt / brkqt: less verbose counterparts to spy / brk
- sc.api/calling-from and :sc/called-from option

## 0.2.1 - 2018-06-12
### Fixed
- Fixed bug causing dispose-all! to fail systematically

## 0.2.0 - 2018-05-29
### Added
- sc.api/last-ep-id
- sc.api/ep-info now accepts ep-id in both integer and vector form
- New arities for sc.api/ep-info and sc.api/loose(-...), which defaults to last-ep-id
- sc.api/ep-value convenience function.
- better validation and error reporting.
### Fixed
- Fixed typo causing save-ep to save bad data.
- file names properly captured when using CLJS (also clean up some CLJS compilation warnings).

## 0.1.4 - 2017-10-19
### Added
- sc.api now requires its own macros in CLJS, making it easier to use in ClojureScript
## 0.1.3 - 2017-10-19
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

[Unreleased]: https://github.com/vvvvalvalval/scope-capture/compare/v0.3.2...HEAD
[0.3.2]: https://github.com/vvvvalvalval/scope-capture/compare/v0.3.0...v0.3.2
[0.3.0]: https://github.com/vvvvalvalval/scope-capture/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/vvvvalvalval/scope-capture/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/vvvvalvalval/scope-capture/compare/v0.1.4...v0.2.0
[0.1.4]: https://github.com/vvvvalvalval/scope-capture/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/vvvvalvalval/scope-capture/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/vvvvalvalval/scope-capture/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/vvvvalvalval/scope-capture/compare/v0.1.0...v0.1.1
