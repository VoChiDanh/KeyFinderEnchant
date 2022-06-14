package gc.tokens.enchants.keyfinder;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

public class ProbabilityCollection<E> {
    protected final Comparator<ProbabilitySetElement<E>> comparator = Comparator.comparingInt(ProbabilitySetElement::getIndex);

    private final TreeSet<ProbabilitySetElement<E>> collection;

    private int totalProbability;

    public ProbabilityCollection() {
        this.collection = new TreeSet<>(this.comparator);
        this.totalProbability = 0;
    }

    public int size() {
        return this.collection.size();
    }

    public boolean isEmpty() {
        return this.collection.isEmpty();
    }

    public boolean contains(E object) {
        if (object == null)
            throw new IllegalArgumentException("Cannot check if null object is contained in a collection");
        return this.collection.stream().anyMatch(entry -> entry.getObject().equals(object));
    }

    public Iterator<ProbabilitySetElement<E>> iterator() {
        return this.collection.iterator();
    }

    public void add(E object, int probability) {
        if (object == null)
            throw new IllegalArgumentException("Cannot add null object");
        if (probability <= 0)
            throw new IllegalArgumentException("Probability must be greater than 0");
        this.collection.add(new ProbabilitySetElement<>(object, probability));
        this.totalProbability += probability;
        updateIndexes();
    }

    public boolean remove(E object) {
        if (object == null)
            throw new IllegalArgumentException("Cannot remove null object");
        Iterator<ProbabilitySetElement<E>> it = iterator();
        boolean removed = it.hasNext();
        while (it.hasNext()) {
            ProbabilitySetElement<E> entry = it.next();
            if (entry.getObject().equals(object)) {
                this.totalProbability -= entry.getProbability();
                it.remove();
            }
        }
        updateIndexes();
        return removed;
    }

    public E get() {
        if (isEmpty())
            throw new IllegalStateException("Cannot get an element out of a empty set");
        ProbabilitySetElement<E> toFind = new ProbabilitySetElement<>(null, 0);
        toFind.setIndex(ThreadLocalRandom.current().nextInt(1, this.totalProbability + 1));
        return Objects.<ProbabilitySetElement<E>>requireNonNull(this.collection.floor(toFind)).getObject();
    }

    public final int getTotalProbability() {
        return this.totalProbability;
    }

    private void updateIndexes() {
        int previousIndex = 0;
        ProbabilitySetElement entry;
        for (Iterator<ProbabilitySetElement<E>> var2 = this.collection.iterator(); var2.hasNext(); previousIndex = entry.setIndex(previousIndex + 1) + entry.getProbability() - 1)
            entry = var2.next();
    }

    static final class ProbabilitySetElement<T> {
        private final T object;

        private final int probability;

        private int index;

        private ProbabilitySetElement(T object, int probability) {
            this.object = object;
            this.probability = probability;
        }

        public T getObject() {
            return this.object;
        }

        public int getProbability() {
            return this.probability;
        }

        public int getIndex() {
            return this.index;
        }

        public int setIndex(int index) {
            this.index = index;
            return index;
        }
    }
}
