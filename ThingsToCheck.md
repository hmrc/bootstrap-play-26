* HttpAuditEventSpec - check if all cases covered
* BaseController and FrontendController - decide what to do
* FrontendAuditFilter - how to deal with 'maskedFormFields' and 'applicationPort' in DI env.
* HeaderCarrierProvider - should it has a new method which takes a Request and figures out internally whether to take session attributes or not.
* move ControllerConfig etc to package: uk.gov.hmrc.play.config + do DI
* remove one of AppName

* review how DeviceIdFilter is used and if it needs changes with accessing from attrs
* fallback for header carrier converter to check attrs
