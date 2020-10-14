package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;

import java.util.Collection;

public interface IOrderIssuer extends IPluginObject {
    boolean addOrderId(OrderType type, int orderId);

    boolean hasOrderId(OrderType type, int orderId);

    boolean removeOrderId(OrderType type, int orderId);

    Collection<Integer> getOrderIds(OrderType type);
}