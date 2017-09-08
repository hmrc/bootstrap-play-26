
# bootstrap-play-25

[![Build Status](https://travis-ci.org/hmrc/bootstrap-play-25.svg?branch=master)](https://travis-ci.org/hmrc/bootstrap-play-25) [ ![Download](https://api.bintray.com/packages/hmrc/releases/bootstrap-play-25/images/download.svg) ](https://bintray.com/hmrc/releases/bootstrap-play-25/_latestVersion)

This library implements basic functionalities required from a platform microservice.


## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "bootstrap-play-25" % "x.x.x"
```

## Configure as a frontend microservice

In your application.conf file, add:

```
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.FrontendModule"
```

And in your SBT build add:

```scala
libraryDependencies += "uk.gov.hmrc" %% "play-ui" % "x.x.x"
libraryDependencies += "uk.gov.hmrc" %% "govuk-template" % "x.x.x"
```

## Configure as a backend microservice

In your application.conf file, add:

```
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.MicroserviceModule"
```

## Default HTTP client

A default http client with pre-configured auditing hook can be injected into any connector. The http client uses http-verbs
For more http-verbs examples see https://github.com/hmrc/http-verbs-example 

```scala
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import javax.inject.Inject

class SomeConnector @Inject() (client: HttpClient) {
  
  client.GET[Option[MyCaseClass]]("http://localhost/my-api")
}
```

## User Authorisation

The library supports user authorisation on microservices

In your application.conf file, add:

```
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuthModule"
```

Your controller will look like this:
```scala
class MyController @Inject() (val authConnector: AuthConnector) extends BaseController with AuthorisedFunctions {
   
   def getSomething(): Action[AnyContent] = Action.async { implicit request ⇒
       authorised() {
         // your protected logic
       } 
   }
 }
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
    
