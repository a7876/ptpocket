package top.zproto.ptpocket.server.datestructure;

import java.util.Random;

/**
 * simply data holder
 * 简易数据
 */
public class DataObject {
    byte[] data;
    int used;

    static final int hashSeed = new Random().nextInt();

    public DataObject(int capacity) {
        data = new byte[capacity];
    }

    public void write(byte[] data, int offset, int length) {
        System.arraycopy(data, offset, data, used, length);
        used += length;
    }

    public void write(byte data) {
        this.data[used++] = data;
    }

    public void writeInt(int num) {
        for (int i = 0; i < 4; i++) {
            data[used++] = (byte) (num >>> (i * 8) & 0xff);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataObject that = (DataObject) o;
        if (that.used != this.used)
            return false;
        for (int i = 0; i < used; i++) {
            if (this.data[i] != that.data[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * murmurHash2 散列函数
     */
    @Override
    public int hashCode() {
        byte[] data = this.data;
        int len = used;
        final int m = 0x5bd1e995;
        final int r = 24;
        int h = hashSeed ^ len;
        int lend4 = len / 4;
        for (int i = 0; i < lend4; i++) {
            int i_4 = i * 4;
            int k = (data[i_4] & 0xff) | ((data[i_4 + 1] & 0xff) << 8) | ((data[i_4 + 2] & 0xff) << 16) | ((data[i_4 + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }
        len %= 4;
        switch (len) {
            case 3:
                h ^= (data[len - 3] & 0xff) << 16;
            case 2:
                h ^= (data[len - 2] & 0xff) << 8;
            case 1:
                h ^= (data[len - 1] & 0xff);
                h *= m;
        }
        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;
        return h;
    }
}

