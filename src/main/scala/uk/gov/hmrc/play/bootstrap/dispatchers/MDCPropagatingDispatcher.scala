/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.bootstrap.dispatchers

import java.util.concurrent.TimeUnit

import akka.dispatch._
import com.typesafe.config.Config
import org.slf4j.MDC

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

class MDCPropagatingDispatcherConfigurator(
                                            config: Config,
                                            prerequisites: DispatcherPrerequisites
                                          ) extends MessageDispatcherConfigurator(config, prerequisites) {

  override def dispatcher(): MessageDispatcher = new MDCPropagatingDispatcher(
    _configurator                  = this,
    id                             = config.getString("id"),
    throughput                     = config.getInt("throughput"),
    throughputDeadlineTime         = FiniteDuration(config.getDuration("throughput-deadline-time", TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS),
    executorServiceFactoryProvider = configureExecutor(),
    shutdownTimeout                = FiniteDuration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  )
}

class MDCPropagatingDispatcher(_configurator: MessageDispatcherConfigurator,
                               id: String,
                               throughput: Int,
                               throughputDeadlineTime: Duration,
                               executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
                               shutdownTimeout: FiniteDuration)
  extends Dispatcher(_configurator, id, throughput, throughputDeadlineTime, executorServiceFactoryProvider, shutdownTimeout) {

  override def execute(runnable: Runnable): Unit =
    prepare().execute(runnable)

  override def prepare(): ExecutionContext = {

    val mdcData = MDC.getCopyOfContextMap

    new ExecutionContext {

      override def execute(runnable: Runnable): Unit = {
        _execute(new Runnable {
          override def run(): Unit = {
            val oldMdcData = MDC.getCopyOfContextMap
            setMDC(mdcData)
            try {
              runnable.run()
            } finally {
              setMDC(oldMdcData)
            }
          }
        })
      }

      override def reportFailure(cause: Throwable): Unit =
        _reportFailure(cause)
    }
  }

  private def _execute(runnable: Runnable): Unit =
    super.execute(runnable)

  private def _reportFailure(cause: Throwable): Unit =
    super.reportFailure(cause)

  private def setMDC(context: java.util.Map[String, String]): Unit = {
    if (context == null) {
      MDC.clear()
    } else {
      MDC.setContextMap(context)
    }
  }
}

