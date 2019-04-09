package org.fok.action.transaction;

import java.util.List;

import org.fok.actionmodel.Action.ActionCommand;
import org.fok.actionmodel.Action.ActionModule;
import org.fok.actionmodel.Action.TransactionHashMessage;
import org.fok.actionmodel.Action.TransactionMessage;
import org.fok.actionmodel.TransactionImpl.TransactionBodyImpl;
import org.fok.actionmodel.TransactionImpl.TransactionInfoImpl;
import org.fok.actionmodel.TransactionImpl.TransactionInputImpl;
import org.fok.actionmodel.TransactionImpl.TransactionNodeImpl;
import org.fok.actionmodel.TransactionImpl.TransactionOutputImpl;
import org.fok.actionmodel.TransactionImpl.TransactionSignatureImpl;
import org.fok.core.FokBlockChain;
import org.fok.core.FokTransaction;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
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
public class GetTransactionByHashImpl extends SessionModules<TransactionHashMessage> {
	@ActorRequire(name = "fok_block_chain_core", scope = "global")
	FokBlockChain fokBlockChain;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;
	@ActorRequire(name = "fok_transaction_core", scope = "global")
	FokTransaction fokTransaction;

	@Override
	public String[] getCmds() {
		return new String[] { ActionCommand.GTH.name() };
	}

	@Override
	public String getModule() {
		return ActionModule.API.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final TransactionHashMessage pb, final CompleteHandler handler) {

//		System.out.println(crypto.verify(crypto.hexStrToBytes(
//				"F91B0786D9A8A4CC422F7E0765A105019F36D839E4B61A3599BAC37E7E47017C5A948ACD8005829EDDDA808924AA74F5CB70FB34C6CD55E3A7E0B37EAB94B346"),
//				crypto.hexStrToBytes(
//						"0a212214cc010dab7dbb5624e894053cb2116701c34adb842a09055de6a779bbac000012210a14402e821c49964c85af87820c40b61cb854eec7531209055de6a779bbac000038d8e091e105"),
//				crypto.hexStrToBytes(
//						"F91B0786D9A8A4CC422F7E0765A105019F36D839E4B61A3599BAC37E7E47017C5A948ACD8005829EDDDA808924AA74F5CB70FB34C6CD55E3A7E0B37EAB94B346CC010DAB7DBB5624E894053CB2116701C34ADB848883F1C3AE75E4B9D84CA87781096F743BC9D5C7D3719FFA492A31D774B9E5185CED38741085C3CC47203C17B4A434C64DCA64E101C743E0382451FD1AC80219")));

		TransactionMessage.Builder tm = TransactionMessage.newBuilder();
		try {
			TransactionInfo oInfo = fokTransaction.getTransaction(crypto.hexStrToBytes(pb.getHash()));
			TransactionInput oInput = oInfo.getBody().getInputs();
			List<TransactionOutput> oOutputs = oInfo.getBody().getOutputsList();

			TransactionInfoImpl.Builder tInfo = TransactionInfoImpl.newBuilder();
			TransactionBodyImpl.Builder tBody = tInfo.getBodyBuilder();
			TransactionInputImpl.Builder tInput = tBody.getInputsBuilder();
			// TransactionOutputImpl.Builder tOutput = tBody.getou
			TransactionNodeImpl.Builder tNode = tInfo.getNodeBuilder();

			tInfo.setHash(crypto.bytesToHexStr(oInfo.getHash().toByteArray()));
			tInfo.setResult(crypto.bytesToHexStr(oInfo.getResult().toByteArray()));

			tBody.setTimestamp(oInfo.getBody().getTimestamp());
			tBody.setType(oInfo.getBody().getType());

			TransactionSignatureImpl.Builder oTransactionSignatureImpl = TransactionSignatureImpl.newBuilder();
			oTransactionSignatureImpl
					.setSignature(crypto.bytesToHexStr(oInfo.getBody().getSignatures().getSignature().toByteArray()));
			tBody.setSignatures(oTransactionSignatureImpl);

			tNode.setAddress(crypto.bytesToHexStr(oInfo.getNode().getAddress().toByteArray()));
			tNode.setNode(oInfo.getNode().getNid());

			tInput.setAddress(crypto.bytesToHexStr(oInput.getAddress().toByteArray()));
			tInput.setNonce(oInput.getNonce());
			tInput.setAmount(BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()).longValue());

			if (oInput.getSymbol() != null && !oInput.getSymbol().equals(ByteString.EMPTY)) {
				tInput.setSymbol(crypto.bytesToHexStr(oInput.getSymbol().toByteArray()));
			}
			if (oInput.getCryptoTokenCount() > 0) {
				for (ByteString cryptoToken : oInput.getCryptoTokenList()) {
					tInput.addCryptoToken(crypto.bytesToHexStr(cryptoToken.toByteArray()));
				}
			}

			if (oOutputs != null && oOutputs.size() > 0) {
				for (int i = 0; i < oOutputs.size(); i++) {
					TransactionOutputImpl.Builder tOutput = tBody.getOutputsBuilder(i);
					tOutput.setAddress(crypto.bytesToHexStr(oOutputs.get(i).getAddress().toByteArray()));
					tOutput.setAmount(
							BytesHelper.bytesToBigInteger(oOutputs.get(i).getAmount().toByteArray()).longValue());
					if (oOutputs.get(i).getCryptoTokenCount() > 0) {
						for (ByteString cryptoToken : oOutputs.get(i).getCryptoTokenList()) {
							tOutput.addCryptoToken(crypto.bytesToHexStr(cryptoToken.toByteArray()));
						}
					}
				}
			}
			tm.setTransactioon(tInfo.build());
			tm.setRetCode(1);
		} catch (Exception e) {
			log.error("", e);
			tm.setRetCode(-1);
			tm.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, tm.build()));
	}
}
