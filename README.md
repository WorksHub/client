# WorksHub

This repo contains the WorksHub frontend code. This is the same code that powers the https://www.works-hub.com/ website.

## The WorksHub technical stack

WorksHub frontend is written in ClojureScript, using [re-frame](https://github.com/Day8/re-frame). It communicates with the server using [GraphQL](https://graphql.org/).

## The open API server

Our server code remains proprietary, and you’re not supposed to be running an instance of it on your machine. Instead, we have set up an open-access API server for your locally running code to use: it is available at https://works-hub-api.herokuapp.com.

Please note that this server only handles API requests and is not by itself a fully-fledged WorksHub instance. If you try to visit it with your browser, you’ll see the message informing you of this.

Please also note that the WorksHub API server is an isolated environment, shared by everyone. It does not and should not store any data about real people, companies and other entities. As a developer, you are responsible for the data you submit to the API server. WorksHub disclaims any responsibility for the data stored therein, to the fullest extent of applicable law.

There are three predefined users, identified by emails:

- `admin@example.com` - an administrator,
- `candidate@example.com` - a candidate user,
- `company@example.com` - a company user.

Anyone can log in as any of these users (there’s no need to check emails in dev mode). Everything is editable. If you need to reset these accounts (or the predefined jobs or blogs), hit the `/api/reset-fixtures` endpoint of the API server.

## Getting started

It is very easy to start hacking on WorksHub frontend. You only need [NPM](https://www.npmjs.com/get-npm) and [Leiningen](https://leiningen.org/).

The first time you prepare to run the app, you need to run

```
npm install
```

From then on, any time you want to run the app itself, run

```
lein repl
> (do (require 'wh.server) (wh.server/start-server))
```

to start simple Ring server. Next start watching ClojureScript files with:

```
npm run dev
```

Now point your browser to: http://localhost:3449 and you should see the WorksHub UI.

(If you're curious, we're using shadow-cljs to watch and compile our frontend. Feel free to run shadow server by yourself and access shadow-cljs Inspect. We have globally available `tap>` in our code. It also means that you can freely install any npm packages with `npm install` and require these in code. Go crazy!)

Now, after a refresh, you should see the landing page.

Click _Login → Send Magic Link_ and enter `candidate@example.com`. You will be logged in immediately.

Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) for information about how to submit pull requests.

## FAQ

### What good is the source code of just the frontend if I don’t have the backend?

Perhaps:

- You’re new to ClojureScript or re-frame and want to play around with the code of a real app deployed in production.
- You are registered with WorksHub and have some problem with our app that you’re willing to try fixing.
- You are registered with WorksHub and interested in working for us. It is _definitely_ a good idea to take a look at our code in this case!
- You are [Richard Stallman](https://www.gnu.org/philosophy/javascript-trap.en.html) and refuse to run non-free JavaScript in your browser.

### How does versioning work? Or building Maven artifacts?

We don’t put version numbers on the client code. The way we’re building it for the real WorksHub instance is we have a master repo (containing server code and a master `project.clj`) which is automatically kept in sync with this repo. This is also why we don’t directly merge the PRs made against this repo: instead, the corresponding commits are made against the master repo and synced back to this one.

### Any tests?

None yet. We do have integration tests, but they run in a dedicated environment and there’s no easy way for us to open-source them. We do have that on our roadmap, though.

### I’ve reloaded my browser and I need to log in again!

That’s expected for now. It may change in the future.

### Logging in via GitHub does not work!

That’s expected too. Use logging in by emails.

### I have another question...

File an issue on GitHub. We also tend to hang out on Clojurians Slack in the #workshub channel.

## Attributions

This list is incomplete:
 - General Icons from Feather Icons (https://feathericons.com/)
 - Social Media Icons from FlatIcon (https://www.flaticon.com/packs/social-networks-logos-2)

## License

Copyright © WorksHub

Distributed under the Eclipse Public License, the same as Clojure.
