package clope.impl;

import clojure.lang.*;

import java.util.Iterator;

public interface Rope extends Seqable, Counted, Iterable, IReduce {

    long size();
    Rope subr(long start, long end);
    Rope append(Rope rope);
    Rope prepend(Rope rope);
    int populate(Object[] arrays, int index);

    static Rope wrap(byte[] b) {
        return new Wrap(b);
    }
    static Rope join(Rope l, Rope r) {
        return l.size() > r.size() ? l.append(r) : r.prepend(l);
    }

    final class Wrap implements Rope {
        final byte[] bytes;
        int hash;

        Wrap(byte[] a) {
            bytes = a;
        }

        @Override
        public int count() {
            return 1;
        }

        @Override
        public ISeq seq() {
            return new PersistentList(bytes);
        }

        @Override
        public Iterator iterator() {
            return new Iterator() {
                boolean done;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public Object next() {
                    done = true;
                    return bytes;
                }
            };
        }

        @Override
        public Object reduce(IFn f) {
            return bytes;
        }

        @Override
        public Object reduce(IFn f, Object init) {
            Object r = f.invoke(init, bytes);
            return r instanceof Reduced ? ((Reduced) r).deref() : r;
        }

        @Override
        public long size() {
            return bytes.length;
        }

        @Override
        public Rope subr(long start, long end) {
            int f = (int) start;
            int s = (int) end - f;
            if (s == bytes.length) return this;
            byte[] b = new byte[s];
            System.arraycopy(bytes, f, b, 0, s);
            return new Wrap(b);
        }

        @Override
        public Rope append(Rope r) {
            return new Join(this, r);
        }

        @Override
        public Rope prepend(Rope r) {
            return new Join(r, this);
        }

        @Override
        public int populate(Object[] arrays, int index) {
            arrays[index] = bytes;
            return index + 1;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                for(byte b: bytes) h = h * 31 + b;
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Rope)) return false;
            Rope r = (Rope) o;
            if (size() != r.size()) return false;
            int i = 0;
            for(Object bs: r)
                for(byte b: (byte []) bs)
                    if (bytes[i++] != b) return false;
            return true;
        }
    }

    final class Join implements Rope {
        final long size;
        final Rope left;
        final Rope right;

        int hash;

        Join(Rope l, Rope r) {
            size = l.size() + r.size();
            left = l;
            right = r;
        }

        @Override
        public int count() {
            return left.count() + right.count();
        }

        @Override
        public ISeq seq() {
            Object[] arrays = new Object[count()];
            populate(arrays, 0);
            return ArraySeq.create(arrays);
        }

        @Override
        public Iterator iterator() {
            Object[] arrays = new Object[count()];
            populate(arrays, 0);
            return new Iterator() {
                int i;

                @Override
                public boolean hasNext() {
                    return i < arrays.length;
                }

                @Override
                public Object next() {
                    return arrays[i++];
                }
            };
        }

        @Override
        public Object reduce(IFn f) {
            int n = count();
            Object[] arrays = new Object[n];
            populate(arrays, 0);
            Object r = arrays[0];
            for(int i = 1; i < n; i++) {
                r = f.invoke(r, arrays[i]);
                if (r instanceof Reduced) return ((Reduced) r).deref();
            }
            return r;
        }

        @Override
        public Object reduce(IFn f, Object r) {
            int n = count();
            Object[] arrays = new Object[n];
            populate(arrays, 0);
            for (int i = 0; i < n; i++) {
                r = f.invoke(r, arrays[i]);
                if (r instanceof Reduced) return ((Reduced) r).deref();
            }
            return r;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = left.hashCode();
                for(Object bs: right)
                    for(byte b: (byte[]) bs)
                        h = h * 31 + b;
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Rope)) return false;
            Rope r = (Rope) o;
            long s = left.size();
            return (size == r.size()) &&
                    left.equals(r.subr(0, s)) &&
                    right.equals(r.subr(s, size));
        }

        @Override
        public int populate(Object[] arrays, int index) {
            return right.populate(arrays, left.populate(arrays, index));
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public Rope subr(long start, long end) {
            if (end - start == size) return this;
            long startk = start - left.size();
            long endk = end - left.size();
            return (startk < 0) ? (endk > 0) ?
                    join(left.subr(start, left.size()), right.subr(0, endk)) :
                    left.subr(start, end) : right.subr(startk, endk);
        }

        @Override
        public Rope append(Rope r) {
            return r.size() < left.size() ? join(left, join(right, r)) : new Join(this, r);
        }

        @Override
        public Rope prepend(Rope r) {
            return r.size() < right.size() ? join(join(r, left), right) : new Join(r, this);
        }
    }
}
