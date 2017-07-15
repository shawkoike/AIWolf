package agent;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/*
 * 全ての役職のベースとなるクラス
 */
public class MyBasePlayer {
	// 自分自身
	Agent me;
	// 日付
	int day;
	// talk() できる時間か? <= 対象：全員
	boolean canTalk;
	// whisper() できる時間か？ <= 対象：人狼
	boolean canWhisper;
	// 最新のゲーム情報
	GameInfo currentGameInfo;
	// 自分以外の生存エージェント
	List<Agent> aliveOthers;
	// 追放されたエージェント
	List<Agent> executedAgents = new ArrayList<>();
	// 殺されたエージェント
	List<Agent> killedAgents = new ArrayList<>();
	// 発言された占い結果報告のリスト
	List<Judge> divinationList = new ArrayList<>();
	// 発言された霊媒結果報告のリスト
	List<Judge> identList = new ArrayList<>();
	// 発言用待ち行列
	Deque<Content> talkQueue = new LinkedList<>();
	// 囁き用待ち行列
	Deque<Content> whisperQueue = new LinkedList<>();
	// 投票先候補
	Agent voteCandidate;
	// 宣言済み投票先候補
	Agent declaredVoteCandidate;
	// 襲撃投票先候補 <= 対象：人狼
	Agent attackVoteCandidate;
	// 宣言済み襲撃投票先候補 <= 対象；人狼
	Agent declaredAttackVoteCandidate;
	// カミングアウト状況
	// エージェントと役職のペアの形でそれまでのカミングアウトを格納
	Map<Agent, Role> comingoutMap = new HashMap<>();
	// GameInfo.talkList 読み込みのヘッド
	int talkListHead;
	// 人間リスト
	List<Agent> humans = new ArrayList<>();
	// 人狼リスト
	List<Agent> wereWolves = new ArrayList<>();

	// エージェントが生きているかどうかを返す
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}

	// エージェントが殺されたかどうかを返す
	protected boolean isKilled(Agent agent) {
		return killedAgents.contains(agent);
	}

	// エージェントがカミングアウトしたかどうかを返す
	protected boolean isComingOut(Agent agent) {
		return comingoutMap.containsKey(agent);
	}

	// 役職がカミングアウトされたかどうかを返す
	protected boolean isComingOut(Role role) {
		return comingoutMap.containsValue(role);
	}

	// エージェントが人間かどうかを返す
	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}

	// エージェントが人狼かどうかを返す
	protected boolean isWereWolf(Agent agent) {
		return wereWolves.contains(agent);
	}

	// リストからランダムに選んで返す
	protected <T> T randomSelect(List<T> list) {
		if(list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	/*
	 * 実際は、MyRoleAssignPlayer が担うため呼ばれることはない
	 */
	public String getName() {
		return "MyBasePlayer";
	}

	/*
	 * ゲーム開始時に呼び出される
	 * 複数回実行対策として、リストを初期化する必要がある
	 */
	protected void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		day = -1;
		me = gameInfo.getAgent();
		aliveOthers = new ArrayList<>(gameInfo.getAliveAgentList());
		// 自分以外の生存者リスト
		aliveOthers.remove(me);
		executedAgents.clear();
		killedAgents.clear();
		divinationList.clear();
		identList.clear();
		comingoutMap.clear();
		humans.clear();
		wereWolves.clear();
	}

	// ゲーム情報の更新
	@SuppressWarnings("incomplete-switch")
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		// 1日の最初の呼び出しは dayStart() の前なので何もしない
		if (currentGameInfo.getDay() == day + 1) {
			day = currentGameInfo.getDay();
			return;
		}
		// 2回目の呼び出し以降
		// （夜限定）追放されたエージェントっを登録
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
		// GameInfo.talkList からカミングアウト・占い報告・霊媒報告を抽出
		for(int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if(talker == me) {
				continue;
			}
			Content content = new Content(talk.getText());
			switch(content.getTopic()) {
			// カミングアウト
			case COMINGOUT:
				comingoutMap.put(talker, content.getRole());
				break;
			// 占い結果の報告
			case DIVINED:
				// Judge（引数）：判定日、判定したエージェント、判定されたエージェント、判定結果
				divinationList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				break;
			// 霊媒結果の報告
			case IDENTIFIED:
				// Judge（引数）：判定日、判定したエージェント、判定されたエージェント、判定結果
				identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				break;
			default:
				break;
			}
		}
		talkListHead = currentGameInfo.getTalkList().size();
	}

	// 1日のスタート
	public void dayStart() {
		// 会話可能
		canTalk = true;
		// 囁き可能
		canWhisper = false;
		// 自分が人狼の場合は囁き可能時間
		if(currentGameInfo.getRole() == Role.WEREWOLF) {
			canWhisper = true;
		}
		talkQueue.clear();
		whisperQueue.clear();
		// 宣言済み投票先候補
		declaredVoteCandidate = null;
		// 投票先候補
		voteCandidate = null;
		// 宣言済み襲撃投票先
		declaredAttackVoteCandidate = null;
		// 襲撃投票先候補
		attackVoteCandidate = null;
		talkListHead = 0;
		// 前日に追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getExecutedAgent());
		// 昨夜死亡した(襲撃された)エージェントがいれば登録
		if(!currentGameInfo.getLastDeadAgentList().isEmpty()) {
			addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
		}
	}

	// 追放されたエージェンを登録
	private void addExecutedAgent(Agent executedAgent) {
		if(executedAgent != null) {
			// 自分以外の生存者から削除
			aliveOthers.remove(executedAgent);
			if(!executedAgents.contains(executedAgent)) {
				// 追放されたエージェントに追加
				executedAgents.add(executedAgent);
			}
		}
	}

	// 襲撃されたエージェントを登録
	private void addKilledAgent(Agent killedAgent) {
		if(killedAgent != null) {
			// 自分以外の生存者から削除
			aliveOthers.remove(killedAgent);
			if(!killedAgents.contains(killedAgent)) {
				// 殺されたエージェントに追加
				killedAgents.add(killedAgent);
			}
		}
	}

	/*
	 * 投票先候補を選び voteCandidate にセットする
	 * 選ぶ基準は、役職によって変わるためここでは仮定義
	 */
	protected void chooseVoteCandidate(){}

	 // chooseVoteCandidate() メソッドで投票候補を決定した後、投票先が変わる場合に投票先を宣言
	public String talk() {
		chooseVoteCandidate();
		// 「投票先候補が null じゃない」 かつ 「投票先候補が宣言済み投票先候補と同じ」
		if(voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}
		return talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
	}

	/*
	 *  襲撃先候補を選び attackVoteCandidate にセットする
	 *  人狼エージェントで実装
	 */
	protected void chooseAttackVoteCandidate(){}

	// chooseAttackVoteCandidate() メソッドで襲撃先候補を決定した後、襲撃先が変わる場合に襲撃先を宣言
	public String whisper() {
		chooseAttackVoteCandidate();
		// 「襲撃先候補が null じゃない」 かつ 「襲撃先候補が宣言済み襲撃先候補と同じ」
		if(attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
			whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
			declaredAttackVoteCandidate = attackVoteCandidate;
		}
		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}

	// 投票行為
	public Agent vote() {
		canTalk = false;
		chooseVoteCandidate();
		return voteCandidate;
	}

	// 襲撃行為
	public Agent attack() {
		canWhisper = false;
		chooseAttackVoteCandidate();
		canWhisper = true;
		return attackVoteCandidate;
	}

	/*
	 * 占い行為
	 * 占い師エージェントで実装
	 */
	public Agent divine() {
		return null;
	}

	/*
	 * 騎士行為
	 * 騎士エージェントで実装
	 */
	public Agent guard() {
		return null;
	}

	// 学習機能用
	public void finish() {}
}
