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

## Future Planned Features

* Display (text-based) files - Attempt to display human read-able files in the
browser, otherwise offer the ability to view them raw.
* Ability to download a snapshot of the repository - This would likely be
checked out at the master branch and pre-zipped to save on bandwidth.

## Contributing

Fork the project and submit merge requests. Otherwise email the author your
patches.

Code that doesn't follow the current formatting, has insufficient commenting,
pulls code from other projects, expands the scope of the project or a variety
of other reasons may be rejected on that basis. If in doubt, contact the author
before hand, otherwise you are free to build your own alternative.
