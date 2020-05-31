<svg width="64" height="64"><polyline points="32,0 0,32 32,64 64,32 32,32 32,48 16,32 32,16 48,32 64,32" fill="#000"/></svg>

# Git Page

A simple stand-alone Git repository viewer written in Java which serves
light-weight HTTP pages, displaying information such as commits, diffs and
providing an RSS feed.

**Warning:** The author makes no promises about security, expose externally at
your own risk.

## Motivation

The number of Git repository viewer platforms is expanding (awesome), but few
offer simplicity, control and RSS feeds about updates.

## Features

* Light-weight (barely valid) HTML responses
* Relatively safe parsing of Git data
* Cuts off long responses
* RSS feeds
* Auto fetching/pulling remote changes from origin
* Page caching

## Future Planned Features

* Bypass CLI wrapper - Running a process from within Java is quite slow,
especially when the data we are retrieving is quite simple. It should be
possible to write a very basic read-only Git client for retrieving the data of
interest.
* Display (text-based) files - Attempt to display human read-able files in the
browser, otherwise offer the ability to view them raw.
* Ability to download a snapshot of the repository - This would likely be
checked out at the master branch and pre-zipped to save on bandwidth.

## Project Structure

```
                +--------------+        +--------------+
                |     Main     |<-------+     JSON     |
                +-----+--------+        +--------------+
                      |
                      v
                +--------------+
                |      Git     |
                +-----+--+-----+
                      |  |
             +--------+  +--------+
             |                    |
             v                    v
    +--------------+        +--------------+
    |    Server    |        |   Maintain   |
    +--------+-----+        +--------------+
             |
             v
    +--------------+
    |  PageBuilder |
    +--------------+
```

## Contributing

Fork the project and submit merge requests. Otherwise email the author your
patches.

Code that doesn't follow the current formatting, has insufficient commenting,
pulls code from other projects, expands the scope of the project or a variety
of other reasons may be rejected on that basis. If in doubt, contact the author
before hand, otherwise you are free to build your own alternative.
