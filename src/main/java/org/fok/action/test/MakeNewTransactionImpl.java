package org.fok.action.test;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.fok.action.model.Test.ActionTestCommand;
import org.fok.action.model.Test.ActionTestModule;
import org.fok.action.model.Test.ReqCreateTransaction;
import org.fok.action.model.Test.ReqMakeNewBlock;
import org.fok.action.model.Test.RespCreateTransaction;
import org.fok.core.FokAccount;
import org.fok.core.FokBlock;
import org.fok.core.FokBlockChain;
import org.fok.core.FokTransaction;
import org.fok.core.bean.TransactionMessage;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Transaction.TransactionBody;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.core.model.Transaction.TransactionSignature;
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
public class MakeNewTransactionImpl extends SessionModules<ReqCreateTransaction> {
	@ActorRequire(name = "fok_account_core", scope = "global")
	FokAccount fokAccount;
	@ActorRequire(name = "fok_block_core", scope = "global")
	FokBlock fokBlock;
	@ActorRequire(name = "fok_block_chain_core", scope = "global")
	FokBlockChain fokBlockChain;
	@ActorRequire(name = "fok_transaction_core", scope = "global")
	FokTransaction fokTransaction;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@Override
	public String[] getCmds() {
		return new String[] { ActionTestCommand.NTT.name() };
	}

	@Override
	public String getModule() {
		return ActionTestModule.ATI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateTransaction pb, final CompleteHandler handler) {
		RespCreateTransaction.Builder oRespCreateTransaction = RespCreateTransaction.newBuilder();

		TransactionInfo.Builder oTransactionInfo = TransactionInfo.newBuilder();
		TransactionBody.Builder oMultiTransactionBody = oTransactionInfo.getBodyBuilder();

		try {
			TransactionInput.Builder oTransactionInput = oMultiTransactionBody.getInputsBuilder();
			oTransactionInput.setAddress(ByteString.copyFrom(crypto.hexStrToBytes(pb.getFromAddress())));
			oTransactionInput
					.setAmount(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
			int nonce;
			if (pb.getNonce() == -1) {
				AccountInfo.Builder ai = fokAccount
						.getAccount(ByteString.copyFrom(crypto.hexStrToBytes(pb.getFromAddress())));
				nonce = ai.getValue().getNonce();
			} else {
				nonce = pb.getNonce();
			}

			oTransactionInput.setNonce(nonce);

			TransactionOutput.Builder oTransactionOutput = TransactionOutput.newBuilder();
			oTransactionOutput.setAddress(ByteString.copyFrom(crypto.hexStrToBytes(pb.getToAddress())));
			oTransactionOutput.setAmount(oTransactionInput.getAmount());

			oMultiTransactionBody.addOutputs(oTransactionOutput);
			if (pb.getTimestamp() != -1) {
				oMultiTransactionBody.setTimestamp(pb.getTimestamp());
			} else {
				oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
			}

			TransactionSignature.Builder oTransactionSignature = oMultiTransactionBody.getSignaturesBuilder();
			oTransactionSignature.setSignature(ByteString.copyFrom(
					crypto.sign(crypto.hexStrToBytes(pb.getPrivKey()), oMultiTransactionBody.build().toByteArray())));

			TransactionMessage tm = fokTransaction.createTransaction(oTransactionInfo);
			oRespCreateTransaction.setHash(crypto.bytesToHexStr(tm.getKey()));
			oRespCreateTransaction.setRetCode(1);
		} catch (Exception ex) {
			log.error("", ex);
			oRespCreateTransaction.setRetCode(-1);
			oRespCreateTransaction.setRetMsg(ex.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateTransaction.build()));
	}
}
