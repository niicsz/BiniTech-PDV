package com.binitech.pdv.config;

public record AuthSessionConfig(long refreshExpiration, String dummyPasswordHash) {}
