package com.tenant.sample;

public class Sample {
    public Integer id;
    public String description;
    
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Sample() {}
    public Sample(Integer id, String description) {
        super();
        this.id = id;
        this.description = description;
    }
}
