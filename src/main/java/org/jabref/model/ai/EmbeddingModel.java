package org.jabref.model.ai;

import org.apache.commons.io.FileUtils;

/**
 * This enumeration was formed by <a href="https://docs.djl.ai/master/docs/load_model.html#list-available-models-using-djl-command-line">listing available embeddings model from Model Zoo of DJL</a>
 * and by using a script that would rewrite the output in Java syntax + calculate size information by finding the models on Hugging Face.
 */
public enum EmbeddingModel {
    ZEROHELL_TINYDPR_ACC_0_315_BS_307("zerohell/tinydpr-acc_0.315-bs_307", 45821255L),
    OMARELSAYEED_SEARCH_MODEL_PRECHATS_AUGMENTED("omarelsayeed/Search_Model_PRECHATS_AUGMENTED", 47407976L),
    DDOBOKKI_ELECTRA_SMALL_NLI_STS("ddobokki/electra-small-nli-sts", 55621715L),
    DAEKEUN_ML_KOELECTRA_SMALL_V3_KORSTS("daekeun-ml/koelectra-small-v3-korsts", 57381425L),
    JINAAI_JINA_EMBEDDING_T_EN_V1("jinaai/jina-embedding-t-en-v1", 58379473L),
    CRAIG_PARAPHRASE_MINILM_L6_V2("Craig/paraphrase-MiniLM-L6-v2", 91599429L),
    FLAX_SENTENCE_EMBEDDINGS_MULTI_QA_V1_MINILM_L6_CLS_DOT("flax-sentence-embeddings/multi-qa_v1-MiniLM-L6-cls_dot", 91634951L),
    FLAX_SENTENCE_EMBEDDINGS_ALL_DATASETS_V4_MINILM_L6("flax-sentence-embeddings/all_datasets_v4_MiniLM-L6", 91649161L),
    OBRIZUM_ALL_MINILM_L6_V2("obrizum/all-MiniLM-L6-v2", 91652211L),
    TAVAKOLIH_ALL_MINILM_L6_V2_PUBMED_FULL("tavakolih/all-MiniLM-L6-v2-pubmed-full", 91838102L),
    TEKRAJ_AVODAMED_SYNONYM_GENERATOR1("tekraj/avodamed-synonym-generator1", 91838145L),
    RIOLITE_PRODUCTS_MATCHING_AUMET_FINE_TUNE_2023_08_22("RIOLITE/products_matching_aumet_fine_tune_2023-08-22", 91839291L),
    JAIMEVERA1107_ALL_MINILM_L6_V2_SIMILARITY_ES("jaimevera1107/all-MiniLM-L6-v2-similarity-es", 91842465L),
    NATEGRO_PARAMETER_MINI_LDS("nategro/parameter-mini-lds", 91843514L),
    VALURANK_MINILM_L6_KEYWORD_EXTRACTION("valurank/MiniLM-L6-Keyword-Extraction", 91900285L),
    COINTEGRATED_RUBERT_TINY("cointegrated/rubert-tiny", 96055211L),
    SEZNAM_RETROMAE_SMALL_CS("Seznam/retromae-small-cs", 99167383L),
    SEZNAM_SIMCSE_DIST_MPNET_CZENG_CS_EN("Seznam/simcse-dist-mpnet-czeng-cs-en", 99167904L),
    SEZNAM_DIST_MPNET_PARACRAWL_CS_EN("Seznam/dist-mpnet-paracrawl-cs-en", 99168001L),
    KORNWTP_CONGEN_WANGCHANBERT_SMALL("kornwtp/ConGen-WangchanBERT-Small", 116473324L),
    MRP_SCT_DISTILLATION_BERT_SMALL("mrp/SCT_Distillation_BERT_Small", 117991002L),
    INKOZIEV_SBERT_PQ("inkoziev/sbert_pq", 120308714L),
    INKOZIEV_SBERT_SYNONYMY("inkoziev/sbert_synonymy", 120311223L),
    THENLPER_GTE_SMALL_ZH("thenlper/gte-small-zh", 121472573L),
    FLAX_SENTENCE_EMBEDDINGS_ALL_DATASETS_V3_MINILM_L12("flax-sentence-embeddings/all_datasets_v3_MiniLM-L12", 134243017L),
    YUNYU_SENTENCE_TRANSFORMERS_E5_SMALL_V2("yunyu/sentence-transformers-e5-small-v2", 134456313L),
    WITH_MADRID_WITH_E5_SMALL_V2("with-madrid/with-e5-small-v2", 134460325L),
    THENLPER_GTE_SMALL("thenlper/gte-small", 134511905L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_ALBERT_BASE_V2("sentence-transformers/paraphrase-albert-base-v2", 142338665L),
    TAYLORAI_BGE_MICRO("TaylorAI/bge-micro", 157041724L),
    TAYLORAI_BGE_MICRO_V2("TaylorAI/bge-micro-v2", 157042922L),
    SENTENCE_TRANSFORMERS_ALL_MINILM_L6_V1("sentence-transformers/all-MiniLM-L6-v1", 182496433L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_ALBERT_SMALL_V2("sentence-transformers/paraphrase-albert-small-v2", 189084665L),
    INFGRAD_STELLA_BASE_ZH_V2("infgrad/stella-base-zh-v2", 205986861L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_MINILM_L3_V2("sentence-transformers/paraphrase-MiniLM-L3-v2", 209503582L),
    INFGRAD_STELLA_BASE_EN_V2("infgrad/stella-base-en-v2", 220059382L),
    COINTEGRATED_RUBERT_TINY2("cointegrated/rubert-tiny2", 238866335L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_BASE_TAS_B("sentence-transformers/msmarco-distilbert-base-tas-b", 266190339L),
    SAKIL_SENTENCE_SIMILARITY_SEMANTIC_SEARCH("Sakil/sentence_similarity_semantic_search", 266463591L),
    INTFLOAT_E5_SMALL_UNSUPERVISED("intfloat/e5-small-unsupervised", 267690846L),
    SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V1("sentence-transformers/all-MiniLM-L12-v1", 267711998L),
    METARANK_CE_ESCI_MINILM_L12_V2("metarank/ce-esci-MiniLM-L12-v2", 268183428L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_MINILM_L6_V2("sentence-transformers/paraphrase-MiniLM-L6-v2", 273472933L),
    SENTENCE_TRANSFORMERS_MULTI_QA_MINILM_L6_DOT_V1("sentence-transformers/multi-qa-MiniLM-L6-dot-v1", 273512223L),
    SENTENCE_TRANSFORMERS_MULTI_QA_MINILM_L6_COS_V1("sentence-transformers/multi-qa-MiniLM-L6-cos-v1", 273514267L),
    SEYEDALI_MULTILINGUAL_TEXT_SEMANTIC_SEARCH_SIAMESE_BERT_V1("SeyedAli/Multilingual-Text-Semantic-Search-Siamese-BERT-V1", 273514358L),
    HUM_WORKS_LODESTONE_BASE_4096_V1("Hum-Works/lodestone-base-4096-v1", 276079125L),
    TAYLORAI_GTE_TINY("TaylorAI/gte-tiny", 295507495L),
    FLAX_SENTENCE_EMBEDDINGS_ST_CODESEARCH_DISTILROBERTA_BASE("flax-sentence-embeddings/st-codesearch-distilroberta-base", 331141227L),
    EMBEDDING_DATA_DISTILROBERTA_BASE_SENTENCE_TRANSFORMER("embedding-data/distilroberta-base-sentence-transformer", 331882136L),
    SENTENCE_TRANSFORMERS_MSMARCO_MINILM_L_6_V3("sentence-transformers/msmarco-MiniLM-L-6-v3", 364329689L),
    SENTENCE_TRANSFORMERS_MSMARCO_MINILM_L6_COS_V5("sentence-transformers/msmarco-MiniLM-L6-cos-v5", 364331688L),
    INTFLOAT_E5_SMALL("intfloat/e5-small", 400847215L),
    BAAI_BGE_SMALL_EN_V1_5("BAAI/bge-small-en-v1.5", 401109346L),
    SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2("sentence-transformers/all-MiniLM-L12-v2", 401240896L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_MINILM_L12_V2("sentence-transformers/paraphrase-MiniLM-L12-v2", 401412737L),
    UER_SBERT_BASE_CHINESE_NLI("uer/sbert-base-chinese-nli", 409269989L),
    GANYMEDENIL_TEXT2VEC_BASE_CHINESE("GanymedeNil/text2vec-base-chinese", 409692389L),
    RYANLAI_SHIBING624_TEXT2VEC_BASE_CHINESE_FINE_TUNE_INSURANCE_DATA("ryanlai/shibing624_text2vec-base-chinese-fine-tune-insurance_data", 409693794L),
    DMETASOUL_SBERT_CHINESE_GENERAL_V2("DMetaSoul/sbert-chinese-general-v2", 409704852L),
    THENLPER_GTE_BASE_ZH("thenlper/gte-base-zh", 409713201L),
    BAAI_BGE_BASE_ZH_V1_5("BAAI/bge-base-zh-v1.5", 409719134L),
    DMETASOUL_SBERT_CHINESE_GENERAL_V1("DMetaSoul/sbert-chinese-general-v1", 410456811L),
    KORNWTP_CONGEN_PARAPHRASE_MULTILINGUAL_MPNET_BASE_V2("kornwtp/ConGen-paraphrase-multilingual-mpnet-base-v2", 423346343L),
    KAMALKRAJ_BIOSIMCSE_BIOLINKBERT_BASE("kamalkraj/BioSimCSE-BioLinkBERT-BASE", 433910574L),
    ADITEYABARAL_SENTENCETRANSFORMER_BERT_BASE_CASED("aditeyabaral/sentencetransformer-bert-base-cased", 433978649L),
    PRITAMDEKA_S_BIOBERT_SNLI_MULTINLI_STSB("pritamdeka/S-BioBert-snli-multinli-stsb", 433980263L),
    PRITAMDEKA_BIOBERT_MNLI_SNLI_SCINLI_SCITAIL_MEDNLI_STSB("pritamdeka/BioBERT-mnli-snli-scinli-scitail-mednli-stsb", 434214702L),
    RICARDO_FILHO_BERT_BASE_PORTUGUESE_CASED_NLI_ASSIN_2("ricardo-filho/bert-base-portuguese-cased-nli-assin-2", 436429150L),
    ALFANEO_JURISBERT_BASE_PORTUGUESE_STS("alfaneo/jurisbert-base-portuguese-sts", 436521076L),
    ANATEL_BERT_AUGMENTED_PT_ANATEL("anatel/bert-augmented-pt-anatel", 436668456L),
    JEGORMEISTER_BERT_BASE_DUTCH_CASED_SNLI("jegormeister/bert-base-dutch-cased-snli", 437355818L),
    TEXTGAIN_ALLNLI_GRONLP_BERT_BASE_DUTCH_CASED("textgain/allnli-GroNLP-bert-base-dutch-cased", 437582891L),
    CASKCSG_COTMAE_BASE_MSMARCO_RETRIEVER("caskcsg/cotmae_base_msmarco_retriever", 438251940L),
    PRITAMDEKA_S_PUBMEDBERT_MS_MARCO_SCIFACT("pritamdeka/S-PubMedBert-MS-MARCO-SCIFACT", 438704594L),
    PRITAMDEKA_S_PUBMEDBERT_MS_MARCO("pritamdeka/S-PubMedBert-MS-MARCO", 438704806L),
    PRITAMDEKA_S_BLUEBERT_SNLI_MULTINLI_STSB("pritamdeka/S-Bluebert-snli-multinli-stsb", 438716287L),
    WHALELOOPS_PHRASE_BERT("whaleloops/phrase-bert", 438716681L),
    MENADSA_S_PUBMEDBERT("menadsa/S-PubMedBERT", 438937759L),
    NEUML_PUBMEDBERT_BASE_EMBEDDINGS("NeuML/pubmedbert-base-embeddings", 438938814L),
    JOSELUHF11_SYMPTOM_ENCODER_V10("joseluhf11/symptom_encoder_v10", 438945770L),
    INOKUFU_BERT_BASE_UNCASED_XNLI_STS_FINETUNED_EDUCATION("inokufu/bert-base-uncased-xnli-sts-finetuned-education", 438946856L),
    TOOLBENCH_TOOLBENCH_IR_BERT_BASED_UNCASED("ToolBench/ToolBench_IR_bert_based_uncased", 438948860L),
    SAP_AI_RESEARCH_MICSE("sap-ai-research/miCSE", 438951368L),
    PRITAMDEKA_PUBMEDBERT_MNLI_SNLI_SCINLI_SCITAIL_MEDNLI_STSB("pritamdeka/PubMedBERT-mnli-snli-scinli-scitail-mednli-stsb", 438952446L),
    DEAN_AI_LEGAL_HEBERT_FT("dean-ai/legal_heBERT_ft", 439070928L),
    ESPEJELOMAR_SENTECE_EMBEDDINGS_BETO("espejelomar/sentece-embeddings-BETO", 440451936L),
    RECOBO_CHEMICAL_BERT_UNCASED_TSDAE("recobo/chemical-bert-uncased-tsdae", 440455185L),
    RECOBO_CHEMICAL_BERT_UNCASED_SIMCSE("recobo/chemical-bert-uncased-simcse", 440455189L),
    EFEDERICI_SENTENCE_BERT_BASE("efederici/sentence-bert-base", 440758981L),
    NICKPROCK_SENTENCE_BERT_BASE_ITALIAN_UNCASED("nickprock/sentence-bert-base-italian-uncased", 440763826L),
    PM_AI_BI_ENCODER_MSMARCO_BERT_BASE_GERMAN("PM-AI/bi-encoder_msmarco_bert-base_german", 440766137L),
    SONOISA_SENTENCE_BERT_BASE_JA_EN_MEAN_TOKENS("sonoisa/sentence-bert-base-ja-en-mean-tokens", 442802550L),
    EMRECAN_BERT_BASE_TURKISH_CASED_MEAN_NLI_STSB_TR("emrecan/bert-base-turkish-cased-mean-nli-stsb-tr", 443305457L),
    BESPIN_GLOBAL_KLUE_SENTENCE_ROBERTA_BASE_KORNLU("bespin-global/klue-sentence-roberta-base-kornlu", 443317038L),
    COLORFULSCOOP_SBERT_BASE_JA("colorfulscoop/sbert-base-ja", 443364095L),
    KDHYUN08_TAACO_STS("KDHyun08/TAACO_STS", 443555816L),
    NYTK_SENTENCE_TRANSFORMERS_EXPERIMENTAL_HUBERT_HUNGARIAN("NYTK/sentence-transformers-experimental-hubert-hungarian", 443593876L),
    DANGVANTUAN_SENTENCE_CAMEMBERT_BASE("dangvantuan/sentence-camembert-base", 444801167L),
    CL_NAGOYA_SUP_SIMCSE_JA_BASE("cl-nagoya/sup-simcse-ja-base", 445137598L),
    ANTOINELOUIS_CROSSENCODER_CAMEMBERT_BASE_MMARCOFR("antoinelouis/crossencoder-camembert-base-mmarcoFR", 445755626L),
    EFEDERICI_CROSS_ENCODER_UMBERTO_STSB("efederici/cross-encoder-umberto-stsb", 445782000L),
    PKSHATECH_SIMCSE_JA_BERT_BASE_CLCMLP("pkshatech/simcse-ja-bert-base-clcmlp", 447508201L),
    DIMITRIZ_ST_GREEK_MEDIA_BERT_BASE_UNCASED("dimitriz/st-greek-media-bert-base-uncased", 453380207L),
    SENTENCE_TRANSFORMERS_ALL_MINILM_L6_V2("sentence-transformers/all-MiniLM-L6-v2", 454845228L),
    BONGSOO_KPF_SBERT_V1_1("bongsoo/kpf-sbert-v1.1", 457312561L),
    SNUNLP_KR_SBERT_V40K_KLUENLI_AUGSTS("snunlp/KR-SBERT-V40K-klueNLI-augSTS", 468422009L),
    JEGORMEISTER_ROBBERT_V2_DUTCH_BASE_MQA_FINETUNED("jegormeister/robbert-v2-dutch-base-mqa-finetuned", 469888693L),
    M3HRDADFI_BERT_ZWNJ_WNLI_MEAN_TOKENS("m3hrdadfi/bert-zwnj-wnli-mean-tokens", 474471928L),
    M3HRDADFI_ROBERTA_ZWNJ_WNLI_MEAN_TOKENS("m3hrdadfi/roberta-zwnj-wnli-mean-tokens", 477438912L),
    ANTOINELOUIS_CROSSENCODER_MMINILMV2_L12_MMARCOFR("antoinelouis/crossencoder-mMiniLMv2-L12-mmarcoFR", 492751134L),
    JMBRITO_PTBR_SIMILARITY_E5_SMALL("jmbrito/ptbr-similarity-e5-small", 492840150L),
    EF_ZULLA_E5_MULTI_SML_TORCH("ef-zulla/e5-multi-sml-torch", 492995658L),
    EFEDERICI_MULTILINGUAL_E5_SMALL_4096("efederici/multilingual-e5-small-4096", 494892908L),
    TURKUNLP_SBERT_UNCASED_FINNISH_PARAPHRASE("TurkuNLP/sbert-uncased-finnish-paraphrase", 498561990L),
    TURKUNLP_SBERT_CASED_FINNISH_PARAPHRASE("TurkuNLP/sbert-cased-finnish-paraphrase", 498571084L),
    FIRQAAA_INDO_SENTENCE_BERT_BASE("firqaaa/indo-sentence-bert-base", 498781876L),
    DATAIKUNLP_PARAPHRASE_MULTILINGUAL_MINILM_L12_V2("DataikuNLP/paraphrase-multilingual-MiniLM-L12-v2", 499614329L),
    VOICELAB_SBERT_BASE_CASED_PL("Voicelab/sbert-base-cased-pl", 500326139L),
    KBLAB_SENTENCE_BERT_SWEDISH_CASED("KBLab/sentence-bert-swedish-cased", 500457410L),
    PRITAMDEKA_S_BIOMED_ROBERTA_SNLI_MULTINLI_STSB("pritamdeka/S-Biomed-Roberta-snli-multinli-stsb", 501284135L),
    MCHOCHLOV_CODEBERT_BASE_CD_FT("mchochlov/codebert-base-cd-ft", 501284494L),
    ANNAWEGMANN_STYLE_EMBEDDING("AnnaWegmann/Style-Embedding", 501287433L),
    IPIPAN_SILVER_RETRIEVER_BASE_V1("ipipan/silver-retriever-base-v1", 501614604L),
    TIMKOORNSTRA_SAURON("TimKoornstra/SAURON", 502033555L),
    SANGMINI_MSMARCO_COTMAE_MINILM_L12_EN_KO_JA("sangmini/msmarco-cotmae-MiniLM-L12_en-ko-ja", 504910521L),
    IMVLADIKON_SENTENCE_TRANSFORMERS_ALEPHBERT("imvladikon/sentence-transformers-alephbert", 505898928L),
    JINAAI_JINA_EMBEDDINGS_V2_SMALL_EN("jinaai/jina-embeddings-v2-small-en", 523027867L),
    SENTENCE_TRANSFORMERS_MULTI_QA_DISTILBERT_DOT_V1("sentence-transformers/multi-qa-distilbert-dot-v1", 531692650L),
    SENTENCE_TRANSFORMERS_MULTI_QA_DISTILBERT_COS_V1("sentence-transformers/multi-qa-distilbert-cos-v1", 531693680L),
    BARISAYDIN_TEXT2VEC_BASE_MULTILINGUAL("barisaydin/text2vec-base-multilingual", 534361171L),
    INTFLOAT_E5_SMALL_V2("intfloat/e5-small-v2", 534806175L),
    SENTENCE_TRANSFORMERS_MSMARCO_MINILM_L_12_V3("sentence-transformers/msmarco-MiniLM-L-12-v3", 534860036L),
    SENTENCE_TRANSFORMERS_MSMARCO_MINILM_L12_COS_V5("sentence-transformers/msmarco-MiniLM-L12-cos-v5", 534862040L),
    SADAKMED_DISTILUSE_BASE_MULTILINGUAL_CASED_V2("sadakmed/distiluse-base-multilingual-cased-v2", 539972541L),
    SADAKMED_DISTILUSE_BASE_MULTILINGUAL_CASED_V1("sadakmed/distiluse-base-multilingual-cased-v1", 539972776L),
    DIGIO_TWITTER4SSE("digio/Twitter4SSE", 541602231L),
    HGTHINKER_VIETNAMESE_SBERT("HgThinker/vietnamese-sbert", 542099329L),
    KEEPITREAL_VIETNAMESE_SBERT("keepitreal/vietnamese-sbert", 542107840L),
    MEDMEDIANI_ARABIC_KW_MDEL("medmediani/Arabic-KW-Mdel", 543394038L),
    DDOBOKKI_KLUE_ROBERTA_SMALL_NLI_STS("ddobokki/klue-roberta-small-nli-sts", 545536446L),
    INFGRAD_STELLA_LARGE_ZH_V2("infgrad/stella-large-zh-v2", 652825863L),
    AMU_TAO_8K("amu/tao-8k", 667548999L),
    MRP_SIMCSE_MODEL_M_BERT_THAI_CASED("mrp/simcse-model-m-bert-thai-cased", 714456826L),
    EZLEE_E_COMMERCE_BERT_BASE_MULTILINGUAL_CASED("EZlee/e-commerce-bert-base-multilingual-cased", 715407892L),
    NBAILAB_NB_SBERT_BASE("NbAiLab/nb-sbert-base", 715413471L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_BASE_V2("sentence-transformers/msmarco-distilbert-base-v2", 797224782L),
    SENTENCE_TRANSFORMERS_QUORA_DISTILBERT_BASE("sentence-transformers/quora-distilbert-base", 797224802L),
    SENTENCE_TRANSFORMERS_DISTILBERT_BASE_NLI_STSB_MEAN_TOKENS("sentence-transformers/distilbert-base-nli-stsb-mean-tokens", 797225157L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_BASE_V4("sentence-transformers/msmarco-distilbert-base-v4", 797225173L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_BASE_V3("sentence-transformers/msmarco-distilbert-base-v3", 797225186L),
    SENTENCE_TRANSFORMERS_NQ_DISTILBERT_BASE_V1("sentence-transformers/nq-distilbert-base-v1", 797225211L),
    SENTENCE_TRANSFORMERS_STSB_DISTILBERT_BASE("sentence-transformers/stsb-distilbert-base", 797225223L),
    SENTENCE_TRANSFORMERS_NLI_DISTILBERT_BASE("sentence-transformers/nli-distilbert-base", 797225321L),
    SENTENCE_TRANSFORMERS_DISTILBERT_BASE_NLI_MEAN_TOKENS("sentence-transformers/distilbert-base-nli-mean-tokens", 797225416L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_COS_V5("sentence-transformers/msmarco-distilbert-cos-v5", 797227197L),
    SENTENCE_TRANSFORMERS_DISTILBERT_BASE_NLI_STSB_QUORA_RANKING("sentence-transformers/distilbert-base-nli-stsb-quora-ranking", 797227940L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_DOT_V5("sentence-transformers/msmarco-distilbert-dot-v5", 797243272L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_BASE_DOT_PROD_V3("sentence-transformers/msmarco-distilbert-base-dot-prod-v3", 801943600L),
    SONOISA_CLIP_VIT_B_32_JAPANESE_V1("sonoisa/clip-vit-b-32-japanese-v1", 803727976L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_TINYBERT_L6_V2("sentence-transformers/paraphrase-TinyBERT-L6-v2", 804365804L),
    KMARIUNAS_UNCASED_BERT_TRIPLET_40("kmariunas/uncased-bert-triplet-40", 876434296L),
    SENTENCE_TRANSFORMERS_FACEBOOK_DPR_CTX_ENCODER_SINGLE_NQ_BASE("sentence-transformers/facebook-dpr-ctx_encoder-single-nq-base", 876666203L),
    SENTENCE_TRANSFORMERS_NLI_BERT_BASE("sentence-transformers/nli-bert-base", 876667005L),
    INTFLOAT_E5_BASE_UNSUPERVISED("intfloat/e5-base-unsupervised", 876669021L),
    SENTENCE_TRANSFORMERS_MSMARCO_BERT_CO_CONDENSOR("sentence-transformers/msmarco-bert-co-condensor", 876675904L),
    DENNLINGER_BERT_WIKI_PARAGRAPHS("dennlinger/bert-wiki-paragraphs", 876684158L),
    SENTENCE_TRANSFORMERS_ALL_MPNET_BASE_V1("sentence-transformers/all-mpnet-base-v1", 876722807L),
    INTFLOAT_E5_BASE("intfloat/e5-base", 876731424L),
    SENTENCE_TRANSFORMERS_ALL_MPNET_BASE_V2("sentence-transformers/all-mpnet-base-v2", 876747501L),
    BIU_NLP_ABSTRACT_SIM_SENTENCE_PUBMED("biu-nlp/abstract-sim-sentence-pubmed", 876931102L),
    HIIAMSID_SENTENCE_SIMILARITY_SPANISH_ES("hiiamsid/sentence_similarity_spanish_es", 879645858L),
    SONOISA_SENTENCE_BERT_BASE_JA_MEAN_TOKENS("sonoisa/sentence-bert-base-ja-mean-tokens", 885270964L),
    SONOISA_SENTENCE_BERT_BASE_JA_MEAN_TOKENS_V2("sonoisa/sentence-bert-base-ja-mean-tokens-v2", 885310545L),
    JHGAN_KO_SBERT_STS("jhgan/ko-sbert-sts", 886044722L),
    JHGAN_KO_SBERT_MULTITASK("jhgan/ko-sbert-multitask", 886045069L),
    JHGAN_KO_SBERT_NLI("jhgan/ko-sbert-nli", 886045384L),
    JHGAN_KO_SROBERTA_STS("jhgan/ko-sroberta-sts", 886050344L),
    JHGAN_KO_SROBERTA_MULTITASK("jhgan/ko-sroberta-multitask", 886051209L),
    NICKPROCK_SENTENCE_BERT_BASE_ITALIAN_XXL_UNCASED("nickprock/sentence-bert-base-italian-xxl-uncased", 886645281L),
    ANTOINELOUIS_BIENCODER_CAMEMBERT_BASE_MMARCOFR("antoinelouis/biencoder-camembert-base-mmarcoFR", 888314464L),
    NETHERLANDSFORENSICINSTITUTE_ROBBERT_2022_DUTCH_SENTENCE_TRANSFORMERS("NetherlandsForensicInstitute/robbert-2022-dutch-sentence-transformers", 954183237L),
    L3CUBE_PUNE_MARATHI_SENTENCE_SIMILARITY_SBERT("l3cube-pune/marathi-sentence-similarity-sbert", 959874042L),
    L3CUBE_PUNE_TAMIL_SENTENCE_BERT_NLI("l3cube-pune/tamil-sentence-bert-nli", 959875603L),
    L3CUBE_PUNE_MALAYALAM_SENTENCE_SIMILARITY_SBERT("l3cube-pune/malayalam-sentence-similarity-sbert", 959875683L),
    L3CUBE_PUNE_INDIC_SENTENCE_BERT_NLI("l3cube-pune/indic-sentence-bert-nli", 959875927L),
    ANTOINELOUIS_BIENCODER_MMINILMV2_L12_MMARCOFR("antoinelouis/biencoder-mMiniLMv2-L12-mmarcoFR", 963385617L),
    SENTENCE_TRANSFORMERS_ALL_DISTILROBERTA_V1("sentence-transformers/all-distilroberta-v1", 988159693L),
    SDADAS_ST_POLISH_PARAPHRASE_FROM_MPNET("sdadas/st-polish-paraphrase-from-mpnet", 1001952602L),
    D0RJ_E5_BASE_EN_RU("d0rj/e5-base-en-ru", 1064431460L),
    BKAI_FOUNDATION_MODELS_VIETNAMESE_BI_ENCODER("bkai-foundation-models/vietnamese-bi-encoder", 1082117217L),
    VOVANPHUC_SUP_SIMCSE_VIETNAMESE_PHOBERT_BASE("VoVanPhuc/sup-SimCSE-VietNamese-phobert-base", 1086863154L),
    SETU4993_LEALLA_SMALL("setu4993/LEALLA-small", 1123385164L),
    AIDA_UPM_MSTSB_PARAPHRASE_MULTILINGUAL_MPNET_BASE_V2("AIDA-UPM/mstsb-paraphrase-multilingual-mpnet-base-v2", 1126421474L),
    LIGHTETERNAL_STSB_XLM_R_GREEK_TRANSFER("lighteternal/stsb-xlm-r-greek-transfer", 1126421894L),
    SYMANTO_SN_XLM_ROBERTA_BASE_SNLI_MNLI_ANLI_XNLI("symanto/sn-xlm-roberta-base-snli-mnli-anli-xnli", 1126437775L),
    KAISERRR_PMC_VIT_L_14_MULTILINGUAL("kaiserrr/pmc-vit-l-14-multilingual", 1134398395L),
    UARITM_MULTILINGUAL_EN_RU_UK("uaritm/multilingual_en_ru_uk", 1134401492L),
    EMBAAS_SENTENCE_TRANSFORMERS_MULTILINGUAL_E5_BASE("embaas/sentence-transformers-multilingual-e5-base", 1134404203L),
    UARITM_MULTILINGUAL_EN_UK_PL_RU("uaritm/multilingual_en_uk_pl_ru", 1134405835L),
    MEEDAN_PARAPHRASE_FILIPINO_MPNET_BASE_V2("meedan/paraphrase-filipino-mpnet-base-v2", 1134417514L),
    LLUKAS22_PARAPHRASE_MULTILINGUAL_MPNET_BASE_V2_EMBEDDING_ALL("LLukas22/paraphrase-multilingual-mpnet-base-v2-embedding-all", 1134852016L),
    EFEDERICI_E5_BASE_MULTILINGUAL_4096("efederici/e5-base-multilingual-4096", 1141989720L),
    SDADAS_MMLW_RETRIEVAL_E5_LARGE("sdadas/mmlw-retrieval-e5-large", 1142073524L),
    SHIBING624_TEXT2VEC_BASE_CHINESE("shibing624/text2vec-base-chinese", 1225878310L),
    CMARKEA_DISTILCAMEMBERT_BASE_NLI("cmarkea/distilcamembert-base-nli", 1233966108L),
    THENLPER_GTE_LARGE_ZH("thenlper/gte-large-zh", 1302780948L),
    SHIBING624_TEXT2VEC_BGE_LARGE_CHINESE("shibing624/text2vec-bge-large-chinese", 1302786763L),
    RESEARCH2NLP_ELECTRICAL_STELLA("Research2NLP/electrical_stella", 1304912784L),
    BAAI_BGE_BASE_EN_V1_5("BAAI/bge-base-en-v1.5", 1312805365L),
    INTFLOAT_E5_BASE_V2("intfloat/e5-base-v2", 1313722035L),
    SENTENCE_TRANSFORMERS_FACEBOOK_DPR_CTX_ENCODER_MULTISET_BASE("sentence-transformers/facebook-dpr-ctx_encoder-multiset-base", 1314862396L),
    SENTENCE_TRANSFORMERS_FACEBOOK_DPR_QUESTION_ENCODER_MULTISET_BASE("sentence-transformers/facebook-dpr-question_encoder-multiset-base", 1314862432L),
    SENTENCE_TRANSFORMERS_FACEBOOK_DPR_QUESTION_ENCODER_SINGLE_NQ_BASE("sentence-transformers/facebook-dpr-question_encoder-single-nq-base", 1314862439L),
    SENTENCE_TRANSFORMERS_STSB_BERT_BASE("sentence-transformers/stsb-bert-base", 1314863267L),
    SENTENCE_TRANSFORMERS_BERT_BASE_WIKIPEDIA_SECTIONS_MEAN_TOKENS("sentence-transformers/bert-base-wikipedia-sections-mean-tokens", 1314863421L),
    SENTENCE_TRANSFORMERS_MSMARCO_BERT_BASE_DOT_V5("sentence-transformers/msmarco-bert-base-dot-v5", 1314884613L),
    BIU_NLP_ABSTRACT_SIM_QUERY_PUBMED("biu-nlp/abstract-sim-query-pubmed", 1314929735L),
    SENTENCE_TRANSFORMERS_NLI_DISTILROBERTA_BASE_V2("sentence-transformers/nli-distilroberta-base-v2", 1316717228L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_DISTILROBERTA_BASE_V2("sentence-transformers/paraphrase-distilroberta-base-v2", 1316717279L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILROBERTA_BASE_V2("sentence-transformers/msmarco-distilroberta-base-v2", 1316717517L),
    SENTENCE_TRANSFORMERS_STSB_DISTILROBERTA_BASE_V2("sentence-transformers/stsb-distilroberta-base-v2", 1316717592L),
    SENTENCE_TRANSFORMERS_ALLENAI_SPECTER("sentence-transformers/allenai-specter", 1320322654L),
    STJIRIS_BERT_LARGE_PORTUGUESE_CASED_LEGAL_TSDAE_GPL_NLI_STS_METAKD_V1("stjiris/bert-large-portuguese-cased-legal-tsdae-gpl-nli-sts-MetaKD-v1", 1338612674L),
    RUFIMELO_BERT_LARGE_PORTUGUESE_CASED_STS("rufimelo/bert-large-portuguese-cased-sts", 1338615398L),
    STJIRIS_BERT_LARGE_PORTUGUESE_CASED_LEGAL_TSDAE_GPL_NLI_STS_V1("stjiris/bert-large-portuguese-cased-legal-tsdae-gpl-nli-sts-v1", 1338619481L),
    EMBAAS_SENTENCE_TRANSFORMERS_E5_LARGE_V2("embaas/sentence-transformers-e5-large-v2", 1341647009L),
    NAUFALIHSAN_INDONESIAN_SBERT_LARGE("naufalihsan/indonesian-sbert-large", 1341648109L),
    DRSEBASTIANK_MEDBEDDING("DrSebastianK/medbedding", 1341655716L),
    THENLPER_GTE_LARGE("thenlper/gte-large", 1341688021L),
    CONSCIOUSAI_CAI_LUNARIS_TEXT_EMBEDDINGS("consciousAI/cai-lunaris-text-embeddings", 1341719299L),
    DENAYA_INDOSBERT_LARGE("denaya/indoSBERT-large", 1342706773L),
    DEUTSCHE_TELEKOM_GBERT_LARGE_PARAPHRASE_COSINE("deutsche-telekom/gbert-large-paraphrase-cosine", 1344052884L),
    DEUTSCHE_TELEKOM_GBERT_LARGE_PARAPHRASE_EUCLIDEAN("deutsche-telekom/gbert-large-paraphrase-euclidean", 1344053075L),
    JHLEE3421_FAQ_SEMANTIC_KLUE_ROBERTA_LARGE("jhlee3421/faq-semantic-klue-roberta-large", 1347759387L),
    YS7YOO_SENTENCE_ROBERTA_LARGE_KOR_STS("ys7yoo/sentence-roberta-large-kor-sts", 1347770951L),
    DANGVANTUAN_CROSSENCODER_CAMEMBERT_LARGE("dangvantuan/CrossEncoder-camembert-large", 1350050334L),
    CL_NAGOYA_SUP_SIMCSE_JA_LARGE("cl-nagoya/sup-simcse-ja-large", 1350138460L),
    CL_NAGOYA_UNSUP_SIMCSE_JA_LARGE("cl-nagoya/unsup-simcse-ja-large", 1350138870L),
    VOICELAB_SBERT_LARGE_CASED_PL("Voicelab/sbert-large-cased-pl", 1422987879L),
    FLAX_SENTENCE_EMBEDDINGS_ALL_DATASETS_V3_ROBERTA_LARGE("flax-sentence-embeddings/all_datasets_v3_roberta-large", 1424216178L),
    COINTEGRATED_RUBERT_BASE_CASED_DP_PARAPHRASE_DETECTION("cointegrated/rubert-base-cased-dp-paraphrase-detection", 1424603089L),
    ABBASGOLESTANI_AG_NLI_DETS_SENTENCE_SIMILARITY_V1("abbasgolestani/ag-nli-DeTS-sentence-similarity-v1", 1424949708L),
    S_NLP_RUBERT_BASE_CASED_CONVERSATIONAL_PARAPHRASE_V1("s-nlp/rubert-base-cased-conversational-paraphrase-v1", 1427358048L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_MULTILINGUAL_MINILM_L12_V2("sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2", 1441155294L),
    INTFLOAT_MULTILINGUAL_E5_SMALL("intfloat/multilingual-e5-small", 1456397601L),
    SHIBING624_TEXT2VEC_BASE_MULTILINGUAL("shibing624/text2vec-base-multilingual", 1475447734L),
    SENTENCE_TRANSFORMERS_ROBERTA_BASE_NLI_MEAN_TOKENS("sentence-transformers/roberta-base-nli-mean-tokens", 1498736923L),
    SENTENCE_TRANSFORMERS_MSMARCO_ROBERTA_BASE_V3("sentence-transformers/msmarco-roberta-base-v3", 1498737534L),
    SENTENCE_TRANSFORMERS_STSB_ROBERTA_BASE("sentence-transformers/stsb-roberta-base", 1498737700L),
    SDADAS_ST_POLISH_PARAPHRASE_FROM_DISTILROBERTA("sdadas/st-polish-paraphrase-from-distilroberta", 1499991122L),
    SENTENCE_TRANSFORMERS_MSMARCO_ROBERTA_BASE_ANCE_FIRSTP("sentence-transformers/msmarco-roberta-base-ance-firstp", 1503475571L),
    SENTENCE_TRANSFORMERS_QUORA_DISTILBERT_MULTILINGUAL("sentence-transformers/quora-distilbert-multilingual", 1619939023L),
    SENTENCE_TRANSFORMERS_DISTILBERT_MULTILINGUAL_NLI_STSB_QUORA_RANKING("sentence-transformers/distilbert-multilingual-nli-stsb-quora-ranking", 1619939159L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_MULTILINGUAL_EN_DE_V2_TMP_LNG_ALIGNED("sentence-transformers/msmarco-distilbert-multilingual-en-de-v2-tmp-lng-aligned", 1619939584L),
    SENTENCE_TRANSFORMERS_MSMARCO_DISTILBERT_MULTILINGUAL_EN_DE_V2_TMP_TRAINED_SCRATCH("sentence-transformers/msmarco-distilbert-multilingual-en-de-v2-tmp-trained-scratch", 1619939629L),
    SENTENCE_TRANSFORMERS_CLIP_VIT_B_32_MULTILINGUAL_V1("sentence-transformers/clip-ViT-B-32-multilingual-v1", 1623088124L),
    SENTENCE_TRANSFORMERS_DISTILUSE_BASE_MULTILINGUAL_CASED_V1("sentence-transformers/distiluse-base-multilingual-cased-v1", 1623089363L),
    COINTEGRATED_LABSE_EN_RU("cointegrated/LaBSE-en-ru", 1722020835L),
    SETU4993_LEALLA_BASE("setu4993/LEALLA-base", 1732164984L),
    SENTENCE_TRANSFORMERS_BERT_BASE_NLI_CLS_TOKEN("sentence-transformers/bert-base-nli-cls-token", 1752798722L),
    SENTENCE_TRANSFORMERS_BERT_BASE_NLI_MAX_TOKENS("sentence-transformers/bert-base-nli-max-tokens", 1752799127L),
    SENTENCE_TRANSFORMERS_BERT_BASE_NLI_STSB_MEAN_TOKENS("sentence-transformers/bert-base-nli-stsb-mean-tokens", 1752799151L),
    HIIAMSID_SENTENCE_SIMILARITY_HINDI("hiiamsid/sentence_similarity_hindi", 1898639316L),
    L3CUBE_PUNE_HINDI_SENTENCE_SIMILARITY_SBERT("l3cube-pune/hindi-sentence-similarity-sbert", 1910125113L),
    SENTENCE_TRANSFORMERS_ROBERTA_BASE_NLI_STSB_MEAN_TOKENS("sentence-transformers/roberta-base-nli-stsb-mean-tokens", 1997326693L),
    SENTENCE_TRANSFORMERS_NLI_ROBERTA_BASE_V2("sentence-transformers/nli-roberta-base-v2", 1997327069L),
    SENTENCE_TRANSFORMERS_MSMARCO_ROBERTA_BASE_V2("sentence-transformers/msmarco-roberta-base-v2", 1997327352L),
    SENTENCE_TRANSFORMERS_STSB_ROBERTA_BASE_V2("sentence-transformers/stsb-roberta-base-v2", 1997327433L),
    SENTENCE_TRANSFORMERS_BERT_BASE_NLI_MEAN_TOKENS("sentence-transformers/bert-base-nli-mean-tokens", 2190792186L),
    CLIPS_MFAQ("clips/mfaq", 2238871436L),
    EMBAAS_SENTENCE_TRANSFORMERS_MULTILINGUAL_E5_LARGE("embaas/sentence-transformers-multilingual-e5-large", 2261845613L),
    SMART_TRIBUNE_SENTENCE_TRANSFORMERS_MULTILINGUAL_E5_LARGE("smart-tribune/sentence-transformers-multilingual-e5-large", 2261845912L),
    PIERLUIGIC_XL_LEXEME("pierluigic/xl-lexeme", 2261859900L),
    SETU4993_LEALLA_LARGE("setu4993/LEALLA-large", 2378725205L),
    GANYMEDENIL_TEXT2VEC_LARGE_CHINESE("GanymedeNil/text2vec-large-chinese", 2604914264L),
    INTFLOAT_E5_LARGE_UNSUPERVISED("intfloat/e5-large-unsupervised", 2682041542L),
    INTFLOAT_E5_LARGE("intfloat/e5-large", 2682104146L),
    LLMRAILS_EMBER_V1("llmrails/ember-v1", 2682324569L),
    INTFLOAT_E5_LARGE_V2("intfloat/e5-large-v2", 2682330013L),
    AARI1995_GERMAN_SEMANTIC_STS_V2("aari1995/German_Semantic_STS_V2", 2687049043L),
    LAJAVANESS_CROSSENCODER_CAMEMBERT_LARGE("Lajavaness/CrossEncoder-camembert-large", 2696718911L),
    SENTENCE_TRANSFORMERS_ROBERTA_LARGE_NLI_MEAN_TOKENS("sentence-transformers/roberta-large-nli-mean-tokens", 2845695742L),
    SENTENCE_TRANSFORMERS_ALL_ROBERTA_LARGE_V1("sentence-transformers/all-roberta-large-v1", 2845707102L),
    D0RJ_E5_LARGE_EN_RU("d0rj/e5-large-en-ru", 2930802136L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_MULTILINGUAL_MPNET_BASE_V2("sentence-transformers/paraphrase-multilingual-mpnet-base-v2", 3351053454L),
    SENTENCE_TRANSFORMERS_PARAPHRASE_XLM_R_MULTILINGUAL_V1("sentence-transformers/paraphrase-xlm-r-multilingual-v1", 3351068034L),
    SENTENCE_TRANSFORMERS_STSB_XLM_R_MULTILINGUAL("sentence-transformers/stsb-xlm-r-multilingual", 3351068164L),
    SENTENCE_TRANSFORMERS_XLM_R_DISTILROBERTA_BASE_PARAPHRASE_V1("sentence-transformers/xlm-r-distilroberta-base-paraphrase-v1", 3351068526L),
    SENTENCE_TRANSFORMERS_XLM_R_100LANGS_BERT_BASE_NLI_STSB_MEAN_TOKENS("sentence-transformers/xlm-r-100langs-bert-base-nli-stsb-mean-tokens", 3351068568L),
    SENTENCE_TRANSFORMERS_XLM_R_BERT_BASE_NLI_STSB_MEAN_TOKENS("sentence-transformers/xlm-r-bert-base-nli-stsb-mean-tokens", 3351068726L),
    INTFLOAT_MULTILINGUAL_E5_BASE("intfloat/multilingual-e5-base", 3378991076L),
    WOODY72_MULTILINGUAL_E5_BASE("woody72/multilingual-e5-base", 3378991184L),
    SETU4993_SMALLER_LABSE("setu4993/smaller-LaBSE", 3512900311L),
    BAAI_BGE_LARGE_EN_V1_5("BAAI/bge-large-en-v1.5", 4019210262L),
    SENTENCE_TRANSFORMERS_BERT_LARGE_NLI_CLS_TOKEN("sentence-transformers/bert-large-nli-cls-token", 4023121298L),
    SENTENCE_TRANSFORMERS_STSB_BERT_LARGE("sentence-transformers/stsb-bert-large", 4023121756L),
    SENTENCE_TRANSFORMERS_NLI_BERT_LARGE_MAX_POOLING("sentence-transformers/nli-bert-large-max-pooling", 4023122032L),
    SENTENCE_TRANSFORMERS_BERT_LARGE_NLI_STSB_MEAN_TOKENS("sentence-transformers/bert-large-nli-stsb-mean-tokens", 4023122049L),
    DANGVANTUAN_SENTENCE_CAMEMBERT_LARGE("dangvantuan/sentence-camembert-large", 4041517356L),
    SENTENCE_TRANSFORMERS_NLI_ROBERTA_LARGE("sentence-transformers/nli-roberta-large", 4267149116L),
    SENTENCE_TRANSFORMERS_BERT_LARGE_NLI_MEAN_TOKENS("sentence-transformers/bert-large-nli-mean-tokens", 5363703243L),
    SENTENCE_TRANSFORMERS_BERT_LARGE_NLI_MAX_TOKENS("sentence-transformers/bert-large-nli-max-tokens", 5363703258L),
    SENTENCE_TRANSFORMERS_LABSE("sentence-transformers/LaBSE", 5668685854L),
    SENTENCE_TRANSFORMERS_USE_CMLM_MULTILINGUAL("sentence-transformers/use-cmlm-multilingual", 5675108503L),
    SENTENCE_TRANSFORMERS_ROBERTA_LARGE_NLI_STSB_MEAN_TOKENS("sentence-transformers/roberta-large-nli-stsb-mean-tokens", 5689102286L),
    SENTENCE_TRANSFORMERS_STSB_ROBERTA_LARGE("sentence-transformers/stsb-roberta-large", 5689102977L),
    INTFLOAT_MULTILINGUAL_E5_LARGE("intfloat/multilingual-e5-large", 6759678687L),
    SETU4993_LABSE("setu4993/LaBSE", 7554057205L),
    BLAXZTER_LABSE_SENTENCE_EMBEDDINGS("Blaxzter/LaBSE-sentence-embeddings", 7554061681L);

    private final String name;
    private final long sizeInBytes;

    EmbeddingModel(String name, long sizeInBytes) {
        this.name = name;
        this.sizeInBytes = sizeInBytes;
    }

    public String getName() {
        return name;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String toString() {
        return name;
    }

    public String fullInfo() {
        return "[" + sizeInfo() + "] " + name;
    }

    public String sizeInfo() {
        return FileUtils.byteCountToDisplaySize(sizeInBytes);
    }
}
