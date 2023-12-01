package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.client.entity.DataWrapper;
import top.zproto.ptpocket.client.entity.Request;
import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.common.Protocol;

/**
 * 客户端命令构建枚举
 * 定义每种命令的构建行为
 */
public enum ClientRequestType implements RequestConverter, CommandType, Protocol {
    SET(CommandType.SET) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + value
            threePart(request, buf);
        }
    }, GET(CommandType.GET) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // key
            onlyKey(request, buf);
        }
    }, H_SET(CommandType.H_SET) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + innerKeyLength + inner key + value
            commonPart(buf);
            DataWrapper[] datas = request.getDatas();
            byte[] key = datas[0].getBytes();
            byte[] innerKey = datas[1].getBytes();
            byte[] value = datas[2].getBytes();
            int bodyLength = 4 + key.length + 4 + innerKey.length + value.length;
            buf.writeInt(bodyLength);
            buf.writeInt(key.length);
            buf.writeBytes(key);
            buf.writeInt(innerKey.length);
            buf.writeBytes(innerKey);
            buf.writeBytes(value);
        }
    }, H_GET(CommandType.H_GET) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + innerKey
            threePart(request, buf);
        }
    }, H_DEL(CommandType.H_DEL) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + innerKey
            threePart(request, buf);
        }
    }, Z_ADD(CommandType.Z_ADD) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + double + value
            commonPart(buf);
            DataWrapper[] datas = request.getDatas();
            byte[] key = datas[0].getBytes();
            double d = datas[1].getdNum();
            byte[] value = datas[2].getBytes();
            int bodyLength = 4 + key.length + 8 + value.length;
            buf.writeInt(bodyLength);
            buf.writeInt(key.length);
            buf.writeBytes(key);
            buf.writeDouble(d);
            buf.writeBytes(value);
        }
    }, Z_DEL(CommandType.Z_DEL) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + value
            threePart(request, buf);
        }
    }, Z_RANGE(CommandType.Z_RANGE) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + int + int
            fourPartWithLastTwoInt(request, buf);
        }
    }, Z_REVERSE_RANGE(CommandType.Z_REVERSE_RANGE) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + int + int
            fourPartWithLastTwoInt(request, buf);
        }
    }, Z_RANGE_SCORE(CommandType.Z_RANGE_SCORE) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + double + double
            commonPart(buf);
            DataWrapper[] datas = request.getDatas();
            byte[] key = datas[0].getBytes();
            double d0 = datas[1].getdNum();
            double d1 = datas[2].getdNum();
            int bodyLength = 4 + key.length + 8 + 8;
            buf.writeInt(bodyLength);
            buf.writeInt(key.length);
            buf.writeBytes(key);
            buf.writeDouble(d0);
            buf.writeDouble(d1);
        }
    }, Z_RANK(CommandType.Z_RANK) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + value
            threePart(request, buf);
        }
    }, Z_REVERSE_RANK(CommandType.Z_REVERSE_RANK) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + value
            threePart(request, buf);
        }
    }, Z_SCORE(CommandType.Z_SCORE) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + value
            threePart(request, buf);
        }
    }, DEL(CommandType.DEL) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // key
            onlyKey(request, buf);
        }
    }, EXPIRE(CommandType.EXPIRE) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + int
            threePartWithOneInt(request, buf);
        }
    }, EXPIRE_MILL(CommandType.EXPIRE_MILL) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // keyLength + key + int
            threePartWithOneInt(request, buf);
        }
    }, PERSIST(CommandType.PERSIST) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            // key
            onlyKey(request, buf);
        }
    }, SELECT(CommandType.SELECT) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            commonPart(buf);
            buf.writeInt(1); // body 长
            buf.writeByte(request.getDatas()[0].getbNum());
        }
    }, STOP(CommandType.STOP) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            commonPart(buf);
            buf.writeInt(0); // no Body
        }
    }, INFO(CommandType.INFO) {
        @Override
        public void convert(Request request, ByteBuf buf) {
            commonPart(buf);
            buf.writeInt(0); // no Body
        }
    };
    final byte instruction;

    ClientRequestType(byte instruction) {
        this.instruction = instruction;
    }

    protected void commonPart(ByteBuf buf) {
        buf.writeInt(MAGIC_NUM);
        buf.writeByte(VERSION);
        buf.writeByte(this.instruction);
    }

    protected void threePart(Request request, ByteBuf buf) {
        // keyLength + key + innerKey / value 的公用部分
        commonPart(buf);
        DataWrapper[] datas = request.getDatas();
        byte[] key = datas[0].getBytes();
        byte[] value = datas[1].getBytes();
        int bodyLength = 4 + key.length + value.length;
        buf.writeInt(bodyLength);
        buf.writeInt(key.length);
        buf.writeBytes(key);
        buf.writeBytes(value);
    }

    protected void fourPartWithLastTwoInt(Request request, ByteBuf buf) {
        commonPart(buf);
        DataWrapper[] datas = request.getDatas();
        // keyLength + key + int + int
        byte[] key = datas[0].getBytes();
        int bodyLength = 4 + key.length + 4 + 4;
        int start = datas[1].getiNum();
        int end = datas[2].getiNum();
        buf.writeInt(bodyLength);
        buf.writeInt(key.length);
        buf.writeBytes(key);
        buf.writeInt(start);
        buf.writeInt(end);
    }

    protected void onlyKey(Request request, ByteBuf buf) {
        commonPart(buf);
        DataWrapper[] datas = request.getDatas();
        byte[] key = datas[0].getBytes();
        buf.writeInt(key.length); // body length
        buf.writeBytes(key);
    }

    protected void threePartWithOneInt(Request request, ByteBuf buf) {
        // keyLength + key + int
        commonPart(buf);
        DataWrapper[] datas = request.getDatas();
        byte[] key = datas[0].getBytes();
        int i = datas[1].getiNum();
        int bodyLength = 4 + key.length + 4;
        buf.writeInt(bodyLength);
        buf.writeInt(key.length);
        buf.writeBytes(key);
        buf.writeInt(i);
    }
}
