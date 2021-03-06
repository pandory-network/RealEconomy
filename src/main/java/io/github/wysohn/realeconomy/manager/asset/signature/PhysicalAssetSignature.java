package io.github.wysohn.realeconomy.manager.asset.signature;

public abstract class PhysicalAssetSignature extends AssetSignature {
    @Override
    public boolean isPhysical() {
        return true;
    }

    public static final String KEY_AMOUNT = "amount";
}
