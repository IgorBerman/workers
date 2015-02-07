package com.worker.shared;

public class WorkMessageArg {

    private String name;
    private Object value;

    public WorkMessageArg(String name, Object value) {
        super();
        this.setName(name);
        this.setValue(value);
    }

    public WorkMessageArg() {
        // for json deserialization
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WorkMessageArg other = (WorkMessageArg) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (value == null) {
            if (other.value != null) return false;
        } else if (!String.valueOf(value).equals(String.valueOf(other.value))) return false;
        return true;
    }

    @Override
    public String toString() {
        return "WorkMessageArg [name=" + name + ", value=" + value + "]";
    }
}
