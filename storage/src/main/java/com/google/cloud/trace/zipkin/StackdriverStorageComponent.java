/*
 * Copyright 2016 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.trace.zipkin;

import static zipkin.storage.StorageAdapters.blockingToAsync;

import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.cloud.trace.zipkin.translation.TraceTranslator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.cognite.metrics.Counter;
import com.cognite.metrics.Registry;
import com.cognite.metrics.server.StandaloneJetty;
import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.AsyncSpanStore;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageComponent;

/**
 * StackdriverStorageComponent is a StorageComponent that consumes spans using the StackdriverSpanConsumer.
 *
 * No SpanStore methods are implemented because read operations are not supported.
 */
public class StackdriverStorageComponent implements StorageComponent, PublicMetrics
{

  private final TraceTranslator traceTranslator;
  private final AsyncSpanConsumer spanConsumer;
  private final ThreadPoolTaskExecutor executor;
  private final LongAdder tracesSent;
  private final Registry registry;
  private final Counter tracesSentCounter;
  private final StandaloneJetty metricsServer;
  private final static int COGNITE_METRICS_PORT = 9100;

  public StackdriverStorageComponent(String projectId, TraceConsumer consumer, ThreadPoolTaskExecutor executor) {
    this.traceTranslator = new TraceTranslator(projectId);
    this.tracesSent = new LongAdder();
    String[] labelValues = new String[] { projectId };
    this.registry = new Registry(true);
    this.tracesSentCounter = new Counter(registry,
        "stackdriver_zipkin", "traces_sent_total",
        "Number of traces sent to Stackdriver Trace",
        new String[] { "project_id" });
    final TraceConsumer instrumentedConsumer = traces ->
    {
      consumer.receive(traces);
      this.tracesSentCounter.increment(
          labelValues, traces != null ? traces.getTracesCount() : 0);
      this.tracesSent.add(traces != null ? traces.getTracesCount() : 0);
    };
    this.spanConsumer = blockingToAsync(new StackdriverSpanConsumer(traceTranslator, instrumentedConsumer), executor);
    this.executor = executor;

    this.metricsServer = new StandaloneJetty(registry, COGNITE_METRICS_PORT);
    try {
      this.metricsServer.start();
    } catch (Exception e) {
      throw new RuntimeException("Failed to start metrics server on port " + COGNITE_METRICS_PORT);
    }
  }

  @Override
  public SpanStore spanStore() {
    throw new UnsupportedOperationException("Read operations are not supported");
  }

  @Override
  public AsyncSpanStore asyncSpanStore() {
    throw new UnsupportedOperationException("Read operations are not supported");
  }

  @Override
  public AsyncSpanConsumer asyncSpanConsumer() {
    return spanConsumer;
  }

  @Override
  public CheckResult check() {
    return CheckResult.OK;
  }

  @Override
  public void close() throws IOException {
    metricsServer.close();
  }

  @Override
  public Collection<Metric<?>> metrics()
  {
    final ArrayList<Metric<?>> result = new ArrayList<>();

    result.add(new Metric<>("gauge.zipkin_storage.stackdriver.active_threads", executor.getActiveCount()));
    result.add(new Metric<>("gauge.zipkin_storage.stackdriver.pool_size", executor.getPoolSize()));
    result.add(new Metric<>("gauge.zipkin_storage.stackdriver.core_pool_size", executor.getCorePoolSize()));
    result.add(new Metric<>("gauge.zipkin_storage.stackdriver.max_pool_size", executor.getMaxPoolSize()));
    result.add(new Metric<>("gauge.zipkin_storage.stackdriver.queue_size", executor.getThreadPoolExecutor().getQueue().size()));
    result.add(new Metric<>("counter.zipkin_storage.stackdriver.sent", tracesSent));

    return result;
  }

  public void resetMetrics() {
    tracesSent.reset();
  }
}
