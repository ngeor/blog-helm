# blog-helm

An example hello world application to show Docker and Helm.

> This project was part of a [blog series](https://ngeor.com/series/2017-12-09-cd-with-helm.html) from 2017.
> **It is outdated and it has been archived**.

Requirements:

- node 10
- minikube 1.4.0
- Helm 2.15.0

## Standalone usage

| Command         | Action                |
| --------------- | --------------------- |
| `npm i`         | Install dependencies  |
| `node index.js` | Run the application   |
| `npm run lint`  | Lint the app          |
| `npm test`      | Run the unit tests    |
| `npm start`     | Start the application |
| `npm run wdio`  | Run the browser tests |

### WebdriverIO

To run tests against an app that already runs on http://localhost:3000/ :

```
npm run wdio
```

To run tests against an app at a different base URL:

```
npm run wdio -- --baseUrl http://some.other.url/
```

## Docker usage

Build the production Docker image with:

```
docker build -t blog-helm .
```

Run the app with:

```
docker run -p 3000:3000 blog-helm
```

To lint the app using Docker:

```
docker run --rm -v $(pwd):/src -w /src node:10-jessie npm run lint
```

For the XML report:

```
docker run --rm -v $(pwd):/src -w /src node:10-jessie npm run lint-junit
```

Correct user permissions if needed with:

```
docker run --rm -v $(pwd):/src -w /src node:10-jessie chown -R $(id -u):$(id -g) test-reports
```

## CI Pipeline

### Commit stage

- Diagnostics (print system info, docker version, current path, user id)
- Ensure feature branch is ahead of master
- Determine version using GitVersion
- Ensure package.json version is aligned with GitVersion and package-lock.json
- npm install
- Linting (ESLint)
- Unit tests with code coverage (mocha/nyc)
- Build Docker image
- Helm init, lint, and package
- Browser tests (webdriver io)

Artifacts:

- Helm chart (\*.tgz)
- Helm environment values overrides (values-\*.yaml)

Using TeamCity:

- the build breaks if code coverage drops
- the commit is tagged if on master branch

### Smoke test

- Smoke test Docker image

### Deployment

- Deploy using Helm
- Post deployment functional tests against actual environments

## Versioning and GitVersion

Version is determined by GitVersion. Builds are tagged on master on the Commit
Stage.

```
docker run --rm -v "$(pwd):/repo" gittools/gitversion:5.0.2-linux-ubuntu-18.04-netcoreapp3.0 /repo /showvariable SemVer
```

## Setting up minikube

### Using TeamCity

This guide shows how to use TeamCity for the build pipeline. We setup TeamCity
inside minikube. This has the advantage of not having to setup a Docker
registry.

### Minikube

Install minikube and start a new cluster with `minikube start`.

**Consider configuring at least 4GB or RAM for the VM. The default 2GB is not
enough.**

Enable the addons dashboard, heapster and ingress.

Install the server-side component of Helm with `helm init`.

### Install TeamCity

Install TeamCity using the helm chart in `./helm/teamcity`.

```
helm install ./helm/teamcity
```

You'll need to edit your HOSTS file to point `teamcity.local` to your local host
(or whatever `minikube ip` returns).

### Configure TeamCity

Visit http://teamcity.local/ to configure TeamCity.

- create admin account
- upload SSH key named "ENVY" which can read/write the git repository
- create TeamCity project named "Blog Helm" (id should be BlogHelm)
- create VCS root for URL `git@github.com:ngeor/blog-helm.git`
  - name "Blog Helm"
  - id "BlogHelm_BlogHelm"
  - branch specification `+:refs/heads/*`
  - authentication method: Uploaded key ("ENVY")
- enable Versioned Settings
  - Synchronization Enabled
  - Project settings VCS root: "Blog Helm"
  - Use Settings from VCS
  - Store secure values outside of VCS
  - Settings format: Kotlin
  - at the prompt, select "Import settings from VCS"
- Set an environment variable on the root project named `KUBECTL_CONFIG` which
  will contain the base64 encoded contents of your kube config. You can create
  that value with `kubectl config view --flatten | base64 -w 0`
- In order to speed up git polling, go to Administration | Global Settings. Set
  the check interval to 30'' and the quiet period to 5''.

TODO npm caching implementation

### DNS

The following commands will allow minikube (and Docker Toolbox if you use it) to
use whatever dummy hosts you add in your `C:\Windows\System32\drivers\etc\hosts`
file.

    # for Docker Toolbox
    C:\Program Files\Oracle\VirtualBox> .\VBoxManage.exe modifyvm default --natdnshostresolver1 on
    # for minikube
    C:\Program Files\Oracle\VirtualBox> .\VBoxManage.exe modifyvm minikube --natdnshostresolver1 on

Reference: https://www.virtualbox.org/manual/ch09.html#nat-adv-dns

Add the following entries to your hosts file:

    # minikube
    192.168.99.101 test.blog-helm.local
    192.168.99.101 acc.blog-helm.local
    192.168.99.101 blog-helm.local

The IP needs to match minikube's IP, which you can get with `minikube ip`.

## Setting up Jenkins

- Install the helm chart (`./helm/jenkins`).
- Go to http://jenkins.local/ (you need to add this to your HOSTS file)
- Copy the admin password from the pod logs and login
- Install suggested plugins
- Create first admin account, continue with configuration
- Install plugin
  [Build Name and Description Setter](https://plugins.jenkins.io/build-name-setter).
- Install plugin SSH Agent.
- Optional: Install Blue Ocean plugin
- Create new multi-branch pipeline project named `blog-helm`
- Define the following checkout behaviours:

  - Fetch tags
  - Checkout to matching local branch
  - Specify ref specs `+refs/heads/*:refs/remotes/@{remote}/*`

- Create a credential named `kubectl_config` (see TeamCity section for info)

TODO Single script build and deploy locally (i.e. without TeamCity but with
minikube)
