package com.api.auto_ease.service.serviceEntry.strategy;

import com.api.auto_ease.domain.appUser.AppUserType;
import com.api.auto_ease.domain.serviceEntry.ServiceEntry;

public interface ServiceEntryStrategy<P> {

    ServiceEntry processServiceEntry(P payload);

    AppUserType getUserType();
}

