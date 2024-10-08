# Send To Repeater, Automatic Tab Naming

_By [Nick Coblentz](https://www.linkedin.com/in/ncoblentz/)_

__This Burp Extension is made possible by [Virtue Security](https://www.virtuesecurity.com), the Application Penetration Testing consulting company I work for.__

## About

This project allows you to send one or more requests to the Repeater and automatically names the tab as:
`HTTPMETHOD /pathinfohere` but removes key phrases like:
- `/api`
- `/v1` or `/v2`
- replaces `/121212121` with `/:num`
- replaces guids/uuids with `/:uuid`

It also allows you to select multiple requests from the proxy history or logger tab and send them to organizer.

### How to Build a Project

#### Setup

This project was initially created using the template found at: https://github.com/ncoblentz/KotlinBurpExtensionBase. That template's README.md describes how to:
- Build this and other projects based on the template
- Load the built jar file in Burp Suite
- Debug Burp Suite extensions using IntelliJ
- Provides links to documentation for building Burp Suite Plugins