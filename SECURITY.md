# Security policy

## Supported versions

Only the newest release gets security fixes. Please update before you report, and check that the problem is still there.

The sync server follows the same rule. Run the newest image, `ghcr.io/not-akari/sumizuri-sync:latest`.

## Report a problem

Please do not open a public issue, and do not post it on Discord.

Report it in private on GitHub: open the [Security tab](https://github.com/not-akari/sumizuri/security) and click **Report a vulnerability**, or go straight to the [report form](https://github.com/not-akari/sumizuri/security/advisories/new).

Please include:

- what the problem is and what someone could do with it
- the steps to make it happen, or a small proof
- the app version, and your device or system
- for the sync server, the image version and how it is set up

## What happens next

- You get an answer within few days.
- Once the problem is confirmed, it is fixed in a new release as soon as possible.
- The report is made public after the fix is out. You are named as the finder, unless you would rather not be.

## What counts

These are in scope:

- the app on Android, iPhone and iPad, Windows, Mac and Linux
- the sync server and its sign in, including two step sign in
- the extension engine, such as an extension that gets out of its sandbox or reads what it should not
- backups, sync data and their encryption
- the app lock
- the updater

These are not in scope:

- sources, repos and extensions made by other people, and the sites they reach
- problems that need a phone or computer that is already unlocked, rooted or taken over
- mistakes in how someone set up their own sync server, such as no HTTPS
- weak spots in tools only used to build the website, which never reach the app or the site
- floods of requests against a server, unless one small request is enough to take it down

## Testing

Please test only on your own install and your own sync server. Do not touch other people's data or servers. Testing done in good faith under these rules is welcome.
