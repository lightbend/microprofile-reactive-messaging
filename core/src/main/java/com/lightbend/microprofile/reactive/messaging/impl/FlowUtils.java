package com.lightbend.microprofile.reactive.messaging.impl;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.Attributes;
import akka.stream.FanInShape2;
import akka.stream.FlowShape;
import akka.stream.Graph;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.Broadcast;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Zip;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.AsyncCallback;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.InHandler;
import akka.stream.stage.OutHandler;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FlowUtils {
  private FlowUtils() {}

  public static <T, R> Flow<Message<T>, Message<R>, NotUsed> bypassFlow(Flow<Message<T>, Message<R>, ?> flow) {
    return Flow.fromGraph(GraphDSL.create(builder -> {
      UniformFanOutShape<Message<T>, Message<T>> bcast = builder.add(Broadcast.create(2));
      FanInShape2<Message<R>, Message<T>, Pair<Message<R>, Message<T>>> zip =
          builder.add(Zip.create());

      builder.from(bcast).via(builder.add(flow)).toInlet(zip.in0());
      builder.from(bcast).toInlet(zip.in1());

      Flow<Pair<Message<R>, Message<T>>, Message<R>, NotUsed> zipF = Flow.<Pair<Message<R>, Message<T>>>create().map(pair ->
          Message.of(pair.first().getPayload(), () ->
              CompletableFuture.allOf(pair.first().ack().toCompletableFuture(), pair.second().ack().toCompletableFuture()))
      );

      return FlowShape.of(bcast.in(), builder.from(zip.out()).via(builder.add(zipF)).out());
    }));
  }

  public static <A, B> Flow<A, B, NotUsed> asynchronouslyProvidedFlow(CompletionStage<Graph<FlowShape<A, B>, NotUsed>> flow) {
    return Flow.fromGraph(new AsynchronouslyProvidedFlow<>(flow));
  }

  private static class AsynchronouslyProvidedFlow<A, B> extends GraphStage<FlowShape<A, B>> {

    private final CompletionStage<Graph<FlowShape<A, B>, NotUsed>> asyncFlow;

    private AsynchronouslyProvidedFlow(CompletionStage<Graph<FlowShape<A, B>, NotUsed>> flow) {
      this.asyncFlow = flow;
    }

    private final Inlet<A> in = Inlet.create("AsyncProvidedFlow.in");
    private final Outlet<B> out = Outlet.create("AsyncProvidedFlow.out");
    private final FlowShape<A, B> shape = FlowShape.of(in, out);

    @Override
    public FlowShape<A,B> shape() {
      return shape;
    }

    @Override
    public GraphStageLogic createLogic(Attributes inheritedAttributes) {

      return new AsyncProvidedFlowLogic();
    }

    private class AsyncProvidedFlowLogic extends GraphStageLogic {

      {
        setHandler(in, new AbstractInHandler() {
          @Override
          public void onPush() throws Exception {
          }
        });
        setHandler(out, new AbstractOutHandler() {
          @Override
          public void onPull() throws Exception {
          }
        });
      }

      public AsyncProvidedFlowLogic() {
        super(AsynchronouslyProvidedFlow.this.shape);
      }

      private SubSinkInlet<B> createSubSinkInlet() {
        return instantiateSubPort(SubSinkInlet.class, "AsyncProvidedFlow.subIn");
      }

      private SubSourceOutlet<A> createSubSourceOutlet() {
        return instantiateSubPort(SubSourceOutlet.class, "AsyncProvidedFlow.subOut");
      }

      /**
       * This hack is necessary due to https://github.com/scala/bug/issues/10889.
       *
       * The problem is, javac recognises the class as a static inner class that you need to pass a reference
       * to the parent to in its constructor, however, it is not a static inner class, and the resulting byte
       * code that it emits is binary incompatible with the actual class.
       */
      private <C> C instantiateSubPort(Class<C> clazz, String name) {
        try {
          return clazz.getDeclaredConstructor(GraphStageLogic.class, String.class).newInstance(this, name);
        }
        catch (Exception e) {
          throw new RuntimeException("Unable to instantiate sub port by reflection", e);
        }
      }

      @Override
      public void preStart() {
        AsyncCallback<Graph<FlowShape<A, B>, NotUsed>> callback = createAsyncCallback(flow -> {

          final SubSinkInlet<B> subIn = createSubSinkInlet();
          subIn.setHandler(new InHandler() {
            @Override
            public void onPush() throws Exception {
              push(out, subIn.grab());
            }

            @Override
            public void onUpstreamFinish() throws Exception {
              complete(out);
            }

            @Override
            public void onUpstreamFailure(Throwable ex) throws Exception {
              fail(out, ex);
            }
          });

          setHandler(out, new OutHandler() {
            @Override
            public void onPull() throws Exception {
              subIn.pull();
            }

            @Override
            public void onDownstreamFinish() throws Exception {
              subIn.cancel();
            }
          });

          final SubSourceOutlet<A> subOut = createSubSourceOutlet();
          subOut.setHandler(new OutHandler() {
            @Override
            public void onDownstreamFinish() throws Exception {
              cancel(in);
            }

            @Override
            public void onPull() throws Exception {
              pull(in);
            }
          });

          setHandler(in, new InHandler() {
            @Override
            public void onPush() throws Exception {
              subOut.push(grab(in));
            }

            @Override
            public void onUpstreamFinish() throws Exception {
              subOut.complete();
            }

            @Override
            public void onUpstreamFailure(Throwable ex) throws Exception {
              subOut.fail(ex);
            }
          });

          Source.fromGraph(subOut.source())
              .via(flow)
              .runWith(subIn.sink(), subFusingMaterializer());

          if (isAvailable(out)) {
            subIn.pull();
          }
        });

        asyncFlow.thenAccept(callback::invoke);
      }
    }
  }


}
