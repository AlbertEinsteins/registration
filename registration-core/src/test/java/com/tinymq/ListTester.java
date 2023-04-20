package com.tinymq;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;


public class ListTester {

    class A {
        private String name;
        private int id;

        public A(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return "A{" +
                    "name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }
    }

    @Test
    public void testA() {
        ArrayList<A> list1 = new ArrayList<>();
        list1.add(new A("1", 1));
        list1.add(new A("2", 2));
        list1.add(new A("3", 3));

        ArrayList<A> list2 = new ArrayList<>(list1);

        list2.remove(0);

        list1.forEach(e -> System.out.printf(e + "\n"));
        list2.forEach(e -> System.out.printf(e + "\n"));
    }
}
