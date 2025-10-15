package _05_02_wildcards_and_bounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class _02_Examples {

    /*
     Wildcards & Bounds quick guide:
     - List<Dog> is NOT a subtype of List<Animal> (invariance).
     - ? extends T (upper bound): "producer" of T; safe to read T, cannot add (except null).
     - ? super T (lower bound): "consumer" of T; safe to add T (and its subtypes), but reads as Object.
     - PECS: Producer Extends, Consumer Super.
     */

    // ----- Domain types for examples -----

    static class Animal implements Comparable<Animal> {
        final String name;

        Animal(String name) {
            this.name = name;
        }

        String speak() {
            return "Animal " + name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + name + ")";
        }

        @Override
        public int compareTo(Animal o) {
            return this.name.compareTo(o.name);
        }
    }

    static class Dog extends Animal {
        Dog(String name) {
            super(name);
        }

        @Override
        String speak() {
            return "Woof! I'm " + name;
        }
    }

    static class Labrador extends Dog {
        Labrador(String name) {
            super(name);
        }

        @Override
        String speak() {
            return "Woof woof! (Labrador) I'm " + name;
        }
    }

    static class Cat extends Animal {
        Cat(String name) {
            super(name);
        }

        @Override
        String speak() {
            return "Meow! I'm " + name;
        }
    }

    // ----- Examples: ? extends (producer) -----

    // Read-only producer of Number: can read Numbers safely, cannot add.
    static double sumNumbers(List<? extends Number> numbers) {
        double sum = 0.0;
        for (Number n : numbers) {
            sum += n.doubleValue();
        }
        return sum;
    }

    // Accepts any producer of Animal: can read Animals, cannot add (except null).
    static void feedAnimals(List<? extends Animal> animals) {
        for (Animal a : animals) {
            // Imagine feeding based on type; here we just print speech.
            System.out.println("Feeding: " + a.speak());
        }
        // animals.add(new Dog("X")); // Does not compile: can't add to ? extends
        // animals.add(new Animal("X")); // Does not compile
        animals.add(null); // The only value allowed to add to a ? extends is null
    }

    // ----- Examples: ? super (consumer) -----

    // Accepts any consumer of Dog: can add Dog or subclass (Labrador).
    static void acceptDogs(List<? super Dog> dogSinks) {
        dogSinks.add(new Dog("Rover"));
        dogSinks.add(new Labrador("Buddy"));
        // dogSinks.add(new Animal("X")); // Does not compile: Animal might be too general
        // Reading is only safe as Object:
        Object first = dogSinks.get(0);
        System.out.println("First in dogSinks (as Object): " + first);
    }

    // Lower-bounded list that consumes integers (and allows adding Integer and its subtypes)
    static void addIntegers(List<? super Integer> sink) {
        sink.add(10);
        sink.add(20);
        sink.add(30);
        // Integer is allowed; Number or Object may appear in the list, but we read as Object:
        Object any = sink.get(0);
        System.out.println("First element from ? super Integer sink (as Object): " + any);
    }

    // ----- PECS in action: copy/merge/stack push/pop -----

    // Copy from producer to consumer: src produces T, dest consumes T
    static <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T t : src) {
            dest.add(t);
        }
    }

    // Merge two producers into one consumer; demonstrates a single type variable across params
    static <T> void merge(List<? extends T> a, List<? extends T> b, List<? super T> out) {
        for (T t : a) out.add(t);
        for (T t : b) out.add(t);
    }

    // Simple generic Stack with pushAll/popAll using PECS
    static class SimpleStack<E> {
        private final Deque<E> stack = new ArrayDeque<>();

        void push(E e) { stack.push(e); }
        E pop() { return stack.pop(); }
        boolean isEmpty() { return stack.isEmpty(); }

        // Producer of E
        void pushAll(Iterable<? extends E> src) {
            for (E e : src) push(e);
        }

        // Consumer of E
        void popAll(Collection<? super E> dst) {
            while (!isEmpty()) {
                dst.add(pop());
            }
        }
    }

    // ----- Wildcard capture: using helper methods -----

    // You cannot set elements on List<?> directly, but you can via a helper that captures the wildcard.
    static void swap(List<?> list, int i, int j) {
        swapHelper(list, i, j);
    }

    private static <T> void swapHelper(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // Reverse using capture helper
    static void reverse(List<?> list) {
        reverseHelper(list);
    }

    private static <T> void reverseHelper(List<T> list) {
        for (int i = 0, j = list.size() - 1; i < j; i++, j--) {
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    // Duplicate the first element using capture helper
    static void duplicateFirst(List<?> list) {
        duplicateFirstHelper(list);
    }

    private static <T> void duplicateFirstHelper(List<T> list) {
        if (!list.isEmpty()) {
            list.add(0, list.get(0));
        }
    }

    // ----- Combining wildcards (nested) -----

    // Flatten a list of lists with different element subtypes into a List<T>
    static <T> List<T> flatten(List<? extends List<? extends T>> lists) {
        List<T> out = new ArrayList<>();
        for (List<? extends T> inner : lists) {
            for (T t : inner) {
                out.add(t);
            }
        }
        return out;
    }

    // ----- Bounds with Comparable and Comparator -----

    // Natural max: elements must be comparable to their supertypes (Comparable<? super T>)
    static <T extends Comparable<? super T>> T maxNatural(List<? extends T> list) {
        if (list.isEmpty()) throw new IllegalArgumentException("empty list");
        T max = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T next = list.get(i);
            if (next.compareTo(max) > 0) {
                max = next;
            }
        }
        return max;
    }

    // Max with Comparator: comparator can compare T or any of its supertypes (Comparator<? super T>)
    static <T> T maxWithComparator(List<? extends T> list, Comparator<? super T> cmp) {
        if (list.isEmpty()) throw new IllegalArgumentException("empty list");
        T max = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T next = list.get(i);
            if (cmp.compare(next, max) > 0) {
                max = next;
            }
        }
        return max;
    }

    // Sort with Comparator using lower-bounded comparator
    static <T> void sortWithComparator(List<T> list, Comparator<? super T> cmp) {
        list.sort(cmp);
    }

    // ----- Unbounded wildcard: read-only as Object -----

    static void printList(String label, List<?> list) {
        System.out.println(label + ": " + list);
        // Object o = list.get(0); // Safe to read as Object
        // list.add(o);            // Not allowed: cannot add to List<?>
    }

    // ----- MAIN: run all examples -----

    public static void main(String[] args) {
        // Invariance: List<Dog> is NOT a List<Animal>
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("Rex"));
        dogs.add(new Labrador("Max"));

        // List<Animal> animals = dogs; // Does not compile (invariance)

        // Covariance with ? extends: producers of Animal
        List<? extends Animal> animalProducers = dogs; // OK
        Animal firstAnimal = animalProducers.get(0);   // OK to read as Animal
        System.out.println("First from ? extends Animal: " + firstAnimal);
        // animalProducers.add(new Dog("Fido")); // Not allowed

        // Contravariance with ? super: consumers of Dog
        List<Animal> animalList = new ArrayList<>();
        List<? super Dog> dogConsumers = animalList; // OK: Animal is a supertype of Dog
        acceptDogs(dogConsumers);
        printList("After acceptDogs (animalList)", animalList);

        // ? extends Number: summing numbers from producers
        List<Integer> ints = Arrays.asList(10, 20, 30);
        List<Double> doubles = Arrays.asList(1.5, 2.5, 3.5);
        System.out.println("sumNumbers(ints): " + sumNumbers(ints));
        System.out.println("sumNumbers(doubles): " + sumNumbers(doubles));

        // Copy/merge (PECS)
        List<Number> numbers = new ArrayList<>();
        copy(ints, numbers);       // Integer -> Number
        copy(doubles, numbers);    // Double -> Number
        printList("Numbers after two copies", numbers);

        List<Cat> cats = Arrays.asList(new Cat("Luna"), new Cat("Milo"));
        merge(dogs, cats, animalList); // T inferred as Animal
        printList("Animals after merge(dogs, cats)", animalList);

        // Lower-bound consumer for integers
        List<Number> numberSink = new ArrayList<>();
        addIntegers(numberSink);
        printList("Number sink after addIntegers", numberSink);

        // SimpleStack with pushAll (? extends) and popAll (? super)
        SimpleStack<Number> numStack = new SimpleStack<>();
        numStack.pushAll(ints);           // pushAll from producer of Number
        numStack.pushAll(doubles);
        List<Object> popped = new ArrayList<>();
        numStack.popAll(popped);          // popAll to consumer of Number
        printList("Popped from stack into Object list", popped);

        // Comparable bound pattern: Comparable<? super T>
        List<Dog> dogList = new ArrayList<>(dogs);
        Dog maxByNatural = maxNatural(dogList); // Animal implements Comparable<Animal>, OK for dogs
        System.out.println("maxNatural(dogs): " + maxByNatural);

        // Comparator with ? super T
        Comparator<Animal> byNameLength = Comparator.comparingInt(a -> a.name.length());
        Dog maxByNameLen = maxWithComparator(dogList, byNameLength);
        System.out.println("maxWithComparator(dogs, byNameLength): " + maxByNameLen);

        // sortWithComparator accepts Comparator<? super Dog>; pass Comparator<Animal>
        sortWithComparator(dogList, byNameLength);
        printList("dogList after sortWithComparator by name length", dogList);

        // Wildcard capture: swap/reverse/duplicateFirst on List<?>
        swap(dogList, 0, 1);
        printList("dogList after swap(0,1)", dogList);

        reverse(dogList);
        printList("dogList after reverse", dogList);

        duplicateFirst(dogList);
        printList("dogList after duplicateFirst", dogList);

        // Nested wildcards: flatten list of lists with heterogeneous element subtypes
        List<Integer> ints2 = Arrays.asList(1, 2);
        List<Double> doubles2 = Arrays.asList(3.14, 2.71);
        List<List<? extends Number>> listOfLists = new ArrayList<>();
        listOfLists.add(ints2);
        listOfLists.add(doubles2);
        List<Number> flattened = flatten(listOfLists);
        printList("Flattened numbers", flattened);

        // Unbounded wildcard: generic printer that accepts any List<?>
        printList("Printing cats via unbounded wildcard", cats);

        // Additional notes (compile-time checks shown as comments):
        // List<Number> nums = new ArrayList<>();
        // List<Integer> ints3 = nums; // Does not compile: invariance
        // List<? super Number> superNum = new ArrayList<Object>();
        // superNum.add(123);      // OK
        // Number n = superNum.get(0); // Does not compile: only safe as Object
        // Object o = superNum.get(0); // OK
    }
}