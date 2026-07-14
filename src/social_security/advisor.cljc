(ns social-security.advisor
  "SocialSecurityAdvisor — proposes a social security administrative
  operation (intake benefits application, verify eligibility checklist,
  log payment record, intake appeal) for a registered claimant. The advisor
  is swappable: `mock-advisor` (deterministic, default in dev/tests/CI)
  or `llm-advisor` (wraps a real `langchain.model/ChatModel`). Either way
  the advisor ONLY produces a PROPOSAL — it never writes to the store and
  has no notion of claimant provenance or benefit determination; the
  `social-security.governor` is the independent system that decides
  whether the proposal may proceed, per the itonami actor pattern.

  A proposal is a map:
    {:op :intake-benefits-application|:verify-eligibility-checklist|:log-payment-record|:intake-appeal
     :effect :propose        ; the advisor NEVER emits a raw store write
     :stake :low|:medium|:high
     :confidence 0.0-1.0
     :rationale str}

  CLOSED ALLOWLIST: the advisor's proposal vocabulary is restricted to
  INTAKE AND ADMINISTRATIVE OPERATIONS ONLY. Proposals touching benefit
  approval/denial, payment disbursement/modification, or claim determination
  are structurally impossible — the advisor cannot construct them. This is
  intentional and non-overridable.

  LLM parse failures always yield `:confidence 0.0` (never fabricate
  confidence), which forces the governor to escalate/hold.")

; Closed allowlist: only these operations are permitted
; NOTE: NO approval/denial/disbursement operations — those are permanently forbidden
(def permitted-ops #{:intake-benefits-application
                      :verify-eligibility-checklist
                      :log-payment-record
                      :intake-appeal})

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer
  "Deterministic mock inference: reads the request's declared op/stake
  straight through (a stand-in for what an LLM would extract from free
  text), with a stake-derived confidence. Enforces closed allowlist:
  only permitted-ops are allowed."
  [_store {:keys [op stake] :as request}]
  (if (contains? permitted-ops op)
    {:op op
     :effect :propose
     :stake (or stake :low)
     :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
     :rationale (str "proposed " (name op) " for claimant " (:claimant-id request))}
    {:op :unknown
     :effect :propose
     :stake :high
     :confidence 0.0
     :rationale "operation not in permitted allowlist (scope boundary)"}))

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a social security administrator advisor. Given a benefits claim
   or administrative request, propose ONLY ONE of these operations:
   :intake-benefits-application, :verify-eligibility-checklist,
   :log-payment-record, or :intake-appeal.

   STRICTLY FORBIDDEN: any operation involving approval/denial of claims,
   modification or disbursement of payments, or final benefit determination.
   If the request implies any of these, respond with :confidence 0.0 and
   :op :unknown. The claimant's eligibility and payment amounts are
   determined EXCLUSIVELY by human staff, never by this advisor.

   Always provide an honest :confidence (0.0-1.0) and a :stake
   (:low/:medium/:high). Never fabricate confidence you don't have.")

(defn- parse-proposal [content]
  (try
    (let [p #?(:clj (read-string content)
               :cljs (js/JSON.parse content))  ; Placeholder for ClojureScript
          op-valid? (and (map? p) (contains? permitted-ops (:op p)))]
      (if op-valid?
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "operation outside permitted allowlist"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  "Wraps a `langchain.model/ChatModel`. `gen-opts` is passed through to
  `model/-generate`. Kept decoupled from any concrete model so this ns
  has no hard dependency beyond `langchain.model`'s protocol."
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "claim request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
