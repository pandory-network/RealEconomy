package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.interfaces.language.ILang;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import modules.MockTransactionHandlerModule;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CentralBankTest extends AbstractBukkitManagerTest {
    CurrencyManager currencyManager;

    List<Module> moduleList = new LinkedList<>();
    private IBankOwnerProvider provider;

    @Before
    public void init() {
        currencyManager = mock(CurrencyManager.class);
        when(currencyManager.get(any(UUID.class))).thenReturn(Optional.empty());

        provider = mock(IBankOwnerProvider.class);
        when(provider.get(any())).thenReturn(mock(IBankOwner.class));

        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            @MaxCapital
            BigDecimal max() {
                return BigDecimal.valueOf(Double.MAX_VALUE);
            }

            @Provides
            @MinCapital
            BigDecimal min() {
                return BigDecimal.valueOf(-Double.MAX_VALUE);
            }
        });
        moduleList.add(new BankOwnerProviderModule(provider));
    }

    @Test
    public void deposit() {
        MockTransactionHandlerModule module = new MockTransactionHandlerModule();
        ITransactionHandler transactionHandler = module.transactionHandler;
        moduleList.add(module);

        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        bank.setBaseCurrency(currency);

        bank.deposit(BigDecimal.valueOf(20304.33), currency);
        // liquidity decreases as currency is collected
        assertEquals(BigDecimal.valueOf(-20304.33), bank.getLiquidity());
        // always max for base currency
        assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), bank.balance(currency));

        // should never invoke the parent method
        verify(transactionHandler, never()).deposit(anyMap(), any(), any());
    }

    @Test
    public void depositNonBase() {
        MockTransactionHandlerModule module = new MockTransactionHandlerModule();
        ITransactionHandler transactionHandler = module.transactionHandler;
        moduleList.add(module);

        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID baseCurrencyUuid = UUID.randomUUID();
        Currency baseCurrency = mock(Currency.class);
        when(baseCurrency.getKey()).thenReturn(baseCurrencyUuid);
        bank.setBaseCurrency(baseCurrency);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        when(transactionHandler.deposit(anyMap(), any(), any())).thenReturn(true);

        bank.deposit(BigDecimal.valueOf(20304.33), currency);
        assertEquals(BigDecimal.ZERO, bank.getLiquidity());

        verify(transactionHandler, times(1))
                .deposit(anyMap(), eq(BigDecimal.valueOf(20304.33)), eq(currency));
    }

    @Test
    public void withdraw() {
        MockTransactionHandlerModule module = new MockTransactionHandlerModule();
        ITransactionHandler transactionHandler = module.transactionHandler;
        moduleList.add(module);

        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        bank.setBaseCurrency(currency);

        bank.withdraw(BigDecimal.valueOf(30567.22), currency);
        // liquidity increases as currency is created
        assertEquals(BigDecimal.valueOf(30567.22), bank.getLiquidity());
        // always max for base currency
        assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), bank.balance(currency));

        // should never invoke the parent method
        verify(transactionHandler, never()).withdraw(anyMap(), any(), any());
    }

    @Test
    public void withdrawNonBase() {
        MockTransactionHandlerModule module = new MockTransactionHandlerModule();
        ITransactionHandler transactionHandler = module.transactionHandler;
        moduleList.add(module);

        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID baseCurrencyUuid = UUID.randomUUID();
        Currency baseCurrency = mock(Currency.class);
        when(baseCurrency.getKey()).thenReturn(baseCurrencyUuid);
        bank.setBaseCurrency(baseCurrency);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        when(transactionHandler.withdraw(anyMap(), any(), any())).thenReturn(true);

        bank.withdraw(BigDecimal.valueOf(30567.22), currency);
        assertEquals(BigDecimal.valueOf(0), bank.getLiquidity());

        // should never invoke the parent method
        verify(transactionHandler, times(1))
                .withdraw(anyMap(), eq(BigDecimal.valueOf(30567.22)), eq(currency), anyBoolean());
    }

    @Test
    public void properties() {
        MockTransactionHandlerModule module = new MockTransactionHandlerModule();
        ITransactionHandler transactionHandler = module.transactionHandler;
        moduleList.add(module);

        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankOwner owner = mock(IBankOwner.class);
        UUID ownerUuid = UUID.randomUUID();
        when(owner.getUuid()).thenReturn(ownerUuid);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        bank.setBankOwner(owner);
        bank.setBaseCurrency(currency);

        Map<ILang, Object> properties = bank.properties();
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_Owner));
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_BaseCurrency));
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_NumAccounts));
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_Liquidity));
    }
}