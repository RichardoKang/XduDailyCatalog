package com.XduDailyCatalog.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;



    public void setData(Object data) {
        this.data = data;
    }


}
