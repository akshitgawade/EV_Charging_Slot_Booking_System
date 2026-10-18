package com.evcharging.action;

public interface Action<T> {

    ActionResult<T> execute() throws Exception;
}
