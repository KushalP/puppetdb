---
title: "PuppetDB 1.5 » API » v3 » Querying PuppetDB Version"
layout: default
canonical: "/puppetdb/latest/api/query/v3/version.html"
---

[curl]: ../curl.html#using-curl-from-localhost-non-sslhttp

The `/version` endpoint can be used to retrieve version information from the PuppetDB server.

## Routes

### `GET /v3/version`

This query endpoint will return version information about the running PuppetDB
server.

#### Examples

[Using `curl` from localhost][curl]:

    curl -X GET http://localhost:8080/v3/version

    {"version": "1.5.2"}

## Response Format

The response will be in `application/json`, and will return a JSON map with a
single key: `version`, whose value is a string representation of the version
of the running PuppetDB server.

    {"version": "1.5.2"}
