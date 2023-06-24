package com.tinymq;

import com.google.inject.*;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiceAppTester {
    private static Injector injector;
    private static CopyOnWriteArrayList<Module> moduleList = new CopyOnWriteArrayList<>();

    public static void registerModule(Module... modules) {
        moduleList.addAll(Arrays.asList(modules));
    }
    public static void run() {
        injector = Guice.createInjector(moduleList.toArray(new Module[0]));
    }

    public static <T> T getInstance(Key<T> key) {
        return injector.getInstance(key);
    }

//    @Singleton
    static class MyService {

        @Inject
        private MyMapper mapper;

        public void solve() {
            this.mapper.pt();
        }

        public MyMapper getMapper() {
            return mapper;
        }
    }


//    @Singleton
    static class MyMapper {
        public void pt() {
            System.out.println("Good");
        }

    }

    static class DemoModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MyMapper.class).in(Scopes.SINGLETON);
            bind(MyService.class);
        }


    }


    public static void main(String[] args) {
        GuiceAppTester.registerModule(new DemoModule());
        GuiceAppTester.run();


        MyService myService = injector.getInstance(MyService.class);
        myService.solve();
        MyService myService2 = injector.getInstance(MyService.class);
        System.out.println(myService2.hashCode() + " " + myService.hashCode());

        MyMapper mapper1 = myService.getMapper();
        MyMapper mapper2 = myService2.getMapper();
        System.out.println(mapper1.hashCode() + " " + mapper2.hashCode());
    }
}
