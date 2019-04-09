package org.fok.action.account;

import org.fok.actionmodel.AccountImpl.AccountCryptoTokenImpl;
import org.fok.actionmodel.AccountImpl.AccountCryptoValueImpl;
import org.fok.actionmodel.AccountImpl.AccountInfoImpl;
import org.fok.actionmodel.AccountImpl.AccountTokenValueImpl;
import org.fok.actionmodel.AccountImpl.AccountValueImpl;
import org.fok.actionmodel.AccountImpl.CryptoTokenImpl;
import org.fok.actionmodel.AccountImpl.CryptoTokenValueImpl;
import org.fok.actionmodel.AccountImpl.TokenImpl;
import org.fok.actionmodel.AccountImpl.TokenValueImpl;
import org.fok.actionmodel.Action.AccountAddressMessage;
import org.fok.actionmodel.Action.AccountMessage;
import org.fok.actionmodel.Action.ActionCommand;
import org.fok.actionmodel.Action.ActionModule;
import org.fok.core.FokAccount;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountCryptoToken;
import org.fok.core.model.Account.AccountCryptoValue;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountTokenValue;
import org.fok.core.model.Account.CryptoTokenValue;
import org.fok.core.model.Account.TokenValue;
import org.fok.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class GetAccountByAddressImpl extends SessionModules<AccountAddressMessage> {
	@ActorRequire(name = "fok_account_core", scope = "global")
	FokAccount fokAccount;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@Override
	public String[] getCmds() {
		return new String[] { ActionCommand.GAT.name() };
	}

	@Override
	public String getModule() {
		return ActionModule.API.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final AccountAddressMessage pb, final CompleteHandler handler) {
		AccountMessage.Builder oAccountMessage = AccountMessage.newBuilder();

		try {
			AccountInfo.Builder oAccount = fokAccount
					.getAccount(ByteString.copyFrom(crypto.hexStrToBytes(pb.getAddress())));

			AccountInfoImpl.Builder aii = AccountInfoImpl.newBuilder();
			aii.setAddress(pb.getAddress());

			AccountValueImpl.Builder avi = aii.getValueBuilder();
			// 账户余额
			avi.setNonce(oAccount.getValue().getNonce());
			avi.setBalance(BytesHelper.bytesToBigInteger(oAccount.getValue().getBalance().toByteArray()).toString());

			if (oAccount.getValue().getTokensCount() > 0) {
				int i = 0;
				for (AccountTokenValue atv : oAccount.getValue().getTokensList()) {
					if (i >= pb.getStartIndex() && i < pb.getEndIndex()) {
						AccountTokenValueImpl.Builder oAccountTokenValueImpl = AccountTokenValueImpl.newBuilder();
						oAccountTokenValueImpl
								.setBalance(BytesHelper.bytesToBigInteger(atv.getBalance().toByteArray()).toString());
						oAccountTokenValueImpl
								.setFreeze(BytesHelper.bytesToBigInteger(atv.getFreeze().toByteArray()).toString());
						oAccountTokenValueImpl
								.setLocked(BytesHelper.bytesToBigInteger(atv.getLocked().toByteArray()).toString());
						oAccountTokenValueImpl.setToken(crypto.bytesToHexStr(atv.getToken().toByteArray()));
						avi.addTokens(oAccountTokenValueImpl);
					} else if (i >= pb.getEndIndex()) {
						break;
					}
					i++;
				}
			}

			if (oAccount.getValue().getCryptosCount() > 0) {
				int i = 0;
				for (AccountCryptoValue acv : oAccount.getValue().getCryptosList()) {
					if (i >= pb.getStartIndex() && i < pb.getEndIndex()) {
						AccountCryptoValueImpl.Builder oAccountCryptoValueImpl = AccountCryptoValueImpl.newBuilder();
						oAccountCryptoValueImpl.setSymbol(crypto.bytesToHexStr(acv.getSymbol().toByteArray()));

						for (AccountCryptoToken act : acv.getTokensList()) {
							AccountCryptoTokenImpl.Builder oAccountCryptoTokenImpl = AccountCryptoTokenImpl
									.newBuilder();

							oAccountCryptoTokenImpl.setCode(act.getCode());
							oAccountCryptoTokenImpl.setExtData(crypto.bytesToHexStr(act.getExtData().toByteArray()));
							oAccountCryptoTokenImpl.setHash(crypto.bytesToHexStr(act.getHash().toByteArray()));
							oAccountCryptoTokenImpl.setIndex(act.getIndex());
							oAccountCryptoTokenImpl.setName(act.getName());
							oAccountCryptoTokenImpl.setNonce(act.getNonce());
							oAccountCryptoTokenImpl.setOwner(crypto.bytesToHexStr(act.getOwner().toByteArray()));
							oAccountCryptoTokenImpl.setTimestamp(act.getTimestamp());
							oAccountCryptoTokenImpl.setTotal(act.getTotal());

							oAccountCryptoValueImpl.addTokens(oAccountCryptoTokenImpl);
						}
						avi.addCryptos(oAccountCryptoValueImpl);
					} else if (i >= pb.getEndIndex()) {
						break;
					}
					i++;
				}
			}
			// 如果是合约账户
			if (oAccount.getValue().getCodeHash() != null
					&& !oAccount.getValue().getCodeHash().equals(ByteString.EMPTY)) {
				avi.setCode(crypto.bytesToHexStr(oAccount.getValue().getCode().toByteArray()));
				avi.setCodeHash(crypto.bytesToHexStr(oAccount.getValue().getCodeHash().toByteArray()));
				avi.setData(crypto.bytesToHexStr(oAccount.getValue().getData().toByteArray()));
			}
			// 如果是联合账户
			if (oAccount.getValue().getAddressCount() > 0) {
				avi.setMax(BytesHelper.bytesToBigInteger(oAccount.getValue().getMax().toByteArray()).toString());
				avi.setAcceptMax(
						BytesHelper.bytesToBigInteger(oAccount.getValue().getAcceptMax().toByteArray()).toString());
				avi.setAcceptLimit(oAccount.getValue().getAcceptLimit());
				for (ByteString subAddress : oAccount.getValue().getAddressList()) {
					avi.addAddress(crypto.bytesToHexStr(subAddress.toByteArray()));
				}
			}
			// 如果存在自己创建的token
			if (oAccount.getValue().getOwnerToken() != null) {
				TokenImpl.Builder ti = TokenImpl.newBuilder();
				for (TokenValue tv : oAccount.getValue().getOwnerToken().getValueList()) {
					TokenValueImpl.Builder oTokenValueImpl = TokenValueImpl.newBuilder();
					oTokenValueImpl.setAddress(crypto.bytesToHexStr(tv.getAddress().toByteArray()));
					oTokenValueImpl.setTimestamp(tv.getTimestamp());
					oTokenValueImpl.setToken(crypto.bytesToHexStr(tv.getToken().toByteArray()));
					oTokenValueImpl.setTotal(BytesHelper.bytesToBigInteger(tv.getTotal().toByteArray()).toString());
					ti.addValue(oTokenValueImpl);
				}
				avi.setOwnerToken(ti);
			}
			// 如果存在自己创建的crypto-token
			if (oAccount.getValue().getOwnerCryptoToken() != null) {
				CryptoTokenImpl.Builder cti = CryptoTokenImpl.newBuilder();
				for (CryptoTokenValue ctv : oAccount.getValue().getOwnerCryptoToken().getValueList()) {
					CryptoTokenValueImpl.Builder oCryptoTokenValueImpl = CryptoTokenValueImpl.newBuilder();
					oCryptoTokenValueImpl.setCurrent(ctv.getCurrent());
					oCryptoTokenValueImpl.setOwner(crypto.bytesToHexStr(ctv.getOwner().toByteArray()));
					oCryptoTokenValueImpl.setSymbol(crypto.bytesToHexStr(ctv.getSymbol().toByteArray()));
					oCryptoTokenValueImpl.setTimestamp(ctv.getTimestamp());
					oCryptoTokenValueImpl.setTotal(ctv.getTotal());
					cti.addValue(oCryptoTokenValueImpl);
				}
				avi.setOwnerCryptoToken(cti);
			}
			aii.setValue(avi);
			oAccountMessage.setRetCode(1);
			oAccountMessage.setAccount(aii);

		} catch (Exception e) {
			log.error("", e);
			oAccountMessage.clear();
			oAccountMessage.setRetCode(-1);
			oAccountMessage.setRetMsg(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oAccountMessage.build()));
	}
}
