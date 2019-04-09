package org.fok.action.test;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.fok.action.model.Test.ActionTestCommand;
import org.fok.action.model.Test.ActionTestModule;
import org.fok.action.model.Test.ReqCreateAccount;
import org.fok.action.model.Test.ReqMakeNewBlock;
import org.fok.action.model.Test.RespCreateAccount;
import org.fok.core.FokAccount;
import org.fok.core.FokBlock;
import org.fok.core.FokBlockChain;
import org.fok.core.crypto.model.KeyPairs;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.unit.UnitHelper;

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
public class MakeNewAccount extends SessionModules<ReqCreateAccount> {
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@ActorRequire(name = "fok_account_core", scope = "global")
	FokAccount fokAccount;

	@Override
	public String[] getCmds() {
		return new String[] { ActionTestCommand.NAC.name() };
	}

	@Override
	public String getModule() {
		return ActionTestModule.ATI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateAccount pb, final CompleteHandler handler) {
		RespCreateAccount.Builder oRespCreateAccount = RespCreateAccount.newBuilder();

		KeyPairs kp = crypto.genAccountKey();
		oRespCreateAccount.setAddress(kp.getAddress());
		oRespCreateAccount.setPrivateKey(kp.getPrikey());
		oRespCreateAccount.setRetCode(1);

		AccountInfo.Builder ai = fokAccount.createAccount(ByteString.copyFrom(crypto.hexStrToBytes(kp.getAddress())));

		ai.getValueBuilder()
				.setBalance(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
		BytesHashMap<AccountInfo.Builder> maps = new BytesHashMap<>();
		maps.put(ai.getAddress().toByteArray(), ai);
		fokAccount.batchPutAccounts(maps);

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateAccount.build()));
	}
}