package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.server.entity.Command;

public enum ServerCommandType implements CommandType, CommandProcessor {
    VERSION_UNSUPPORTED(RESERVED_0) {
        @Override
        public void processCommand(Command command) {

        }
    }, UNKNOWN_COMMAND(RESERVED_1) {
        @Override
        public void processCommand(Command command) {

        }
    }, DEL(CommandType.DEL) {
        @Override
        public void processCommand(Command command) {

        }
    }, EXPIRE(CommandType.EXPIRE) {
        @Override
        public void processCommand(Command command) {

        }
    }, EXPIRE_MILL(CommandType.EXPIRE_MILL) {
        @Override
        public void processCommand(Command command) {

        }
    }, SELECT(CommandType.SELECT) {
        @Override
        public void processCommand(Command command) {

        }
    }, PERSIST(CommandType.PERSIST) {
        @Override
        public void processCommand(Command command) {

        }
    }, STOP(CommandType.STOP) {
        @Override
        public void processCommand(Command command) {

        }
    }, GET(CommandType.GET) {
        @Override
        public void processCommand(Command command) {

        }
    }, SET(CommandType.SET) {
        @Override
        public void processCommand(Command command) {

        }
    }, H_SET(CommandType.H_SET) {
        @Override
        public void processCommand(Command command) {

        }
    }, H_GET(CommandType.H_GET) {
        @Override
        public void processCommand(Command command) {

        }
    }, H_DEL(CommandType.H_DEL) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_ADD(CommandType.Z_ADD) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_DEL(CommandType.Z_DEL) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_RANGE(CommandType.Z_RANGE) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_RANGE_SCORE(CommandType.Z_RANGE_SCORE) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_RANK(CommandType.Z_RANK) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_REVERSE_RANK(CommandType.Z_REVERSE_RANK) {
        @Override
        public void processCommand(Command command) {

        }
    }, Z_SCORE(CommandType.Z_SCORE) {
        @Override
        public void processCommand(Command command) {

        }
    };
    final byte instruction;

    ServerCommandType(byte instruction) {
        this.instruction = instruction;
    }
}
