package com.worker.shared;

public enum JoinState {
    STARTED, ENDED;

    public JoinState opposite() {
        switch(this) {
            case STARTED : return ENDED;
            case ENDED   : return STARTED;
            default: throw new IllegalStateException("not supported join state " + this);
        }
    }
}
