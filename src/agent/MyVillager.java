package agent;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

// 村人役エージェントクラス
public class MyVillager extends MyBasePlayer{

	// 投票先選択
	protected void chooseVoteCandidate() {
		wereWolves.clear();
		// 発言された占い結果報告のリストを個々に取り出す
		for (Judge j:divinationList) {
			// 自分あるいは殺されたエージェントを人狼と判定した占い師を人狼と睨む => 生存してる自称占い師を投票先候補とする
			// 「占い師の判定結果が人狼」 && (「自分を人狼と占った（村人なので嘘と判明）」 || 「人狼に殺された人を人狼と占った（嘘と判明）」)
			if(j.getResult() == Species.WEREWOLF && (j.getTarget() == me || isKilled(j.getTarget()))){
				Agent candidate = j.getAgent();
				if(isAlive(candidate) && !wereWolves.contains(candidate)) {
					// 人狼リストに追加
					wereWolves.add(candidate);
				}
			}
		}
		// 候補がいない場合はランダム
		if(wereWolves.isEmpty()) {
			// 生存者に投票先候補が含まれていない
			if(!aliveOthers.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers);
			}
		}else {
			// 人狼リストに投票先候補がいない
			if(!wereWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(wereWolves);
				// 以前の投票先から変わる場合、新たに推測発言と占い申請を行う
				if(canTalk) {
					// 推測発言
					// EstimateContentBuilder：
					talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
					// 占い申請
					talkQueue.offer(new Content(new RequestContentBuilder(null,
							new Content(new DivinationContentBuilder(voteCandidate)))));
				}
			}
		}
	}

	/*
	 *  囁き行為
	 *  行為主ではないため例外
	 */
	public String whisper() {
		throw new UnsupportedOperationException();
	}

	/*
	 * 襲撃行為
	 * 行為主ではないため例外
	 */
	public Agent attack() {
		throw new UnsupportedOperationException();
	}

	/*
	 * 占い行為
	 * 行為主ではないため例外
	 */
	public Agent divine() {
		throw new UnsupportedOperationException();
	}

	/*
	 * 騎士行為
	 * 行為主ではないため例外
	 */
	public Agent guard() {
		throw new UnsupportedOperationException();
	}
}
