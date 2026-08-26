package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {

  public List<Store> listAllOrderedByName() {
    return listAll(Sort.by("name"));
  }
}