# Contributing to WorksHub

So you’ve looked at our code and want to contribute. That’s great!

## Contributor Agreement

First, you need to sign the WorksHub Contributor Agreement. _TODO: insert instructions how to do it_

Note that we don’t require the Agreement for our other open-source libraries. It’s needed to contribute to this frontend code only.

## Registering on the WorksHub platform

If you don’t have a WorksHub account yet, [create one now](https://functional.works-hub.com/get-started). This is, strictly speaking, an optional step, but we highly recommend it. You’ll need an account to get paid for contributing.

## Finding something to work on

Browse the [list of open issues](https://functional.works-hub.com/issues/workshub-f0774). This list is periodically synced with the [GitHub issue tracker](http://github.com/WorksHub/wh-client/issues).

When you’ve selected something that you’d like to take a crack on, click _Start Work_. You’ll be presented with a dialog that will take you from there.
If you have any questions or comments, please tell us on GitHub in the issue comments.

## Sending a pull request

In order to contribute to this open source repository, ensure the following:

1. Fork this repository
2. Add this repository as `upstream` with `git remote add upstream git@github.com:WorksHub/client-app.git`
2. In your fork, ensure that your local master branch is **always** up to date with this repository master branch.
This comes for free when you fork the repository. If not, give 

   ```git fetch upstream && git checkout master && git reset --hard upstream/master```

3. Checkout a new branch and add your commits there.
4. When you are ready, open a PR in this repository.
4. When you are changes are deemed ok, a maintainer will comment the PR with `OK TO MERGE`. That will trigger our automated pipelines and your commit will make it upstream to our private server.
5. When your changes are merged, you will receive a comment in your PR. After that ensure that your `master` branch is up to date with the master of this repo, by repeating step 2.

## Tips on getting your code merged

Please make sure that:

 - Your code conforms to the [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide)
 - Your PR doesn’t include any sensitive information, such as private keys
 - Any changes you’ve made to the UI look good on both desktop and mobile screens

Once your PR is submitted, we’ll review it and run our internal tests against it. If we find anything to fix, we’ll tell you. Otherwise, we’ll merge the PR and – if the issue carries a monetary value – we’ll pay you.

## Code of conduct

WorksHub strives to provide a safe, welcoming space for contributors.

When contributing, please follow the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/1/4/code-of-conduct). Please report any abusive or otherwise unacceptable behaviour to hello@works-hub.com.
