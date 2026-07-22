#!/usr/bin/env python3
"""一键生成所有模块的 translations JSON 文件"""
import json, os

BASE = r"C:\Users\Work\AndroidStudioProjects\MineAndroid"

modules = {}

# ============ board/module/home ============
modules["board/module/home"] = {
    "zh-CN": {"str_soft_list":"应用列表","主页":"主页应用管理","str_osd_soft_manager":"主页应用管理","str_finish":"完成","home_wallpaper_settings":"壁纸设置"},
    "en":    {"str_soft_list":"App List","主页":"Home App Manager","str_osd_soft_manager":"Home App Manager","str_finish":"Done","home_wallpaper_settings":"Wallpaper Settings"},
    "ru":    {"str_soft_list":"Список приложений","主页":"Управление приложениями","str_osd_soft_manager":"Управление приложениями","str_finish":"Готово","home_wallpaper_settings":"Настройки обоев"},
    "pt":    {"str_soft_list":"Lista de aplicativos","主页":"Gerenciador de apps","str_osd_soft_manager":"Gerenciador de apps","str_finish":"Concluído","home_wallpaper_settings":"Configurações de papel de parede"},
}

# ============ board/module/wallpaper ============
modules["board/module/wallpaper"] = {
    "zh-CN": {"app_wallpaper":"Board Wallpaper","wallpaper_description":"Board动态壁纸","setting_wallpaper_path":"壁纸路径","setting_wallpaper_path_hint":"输入壁纸文件路径","setting_type_video":"视频壁纸","setting_type_image":"图片/轮播壁纸","setting_type_gl":"OpenGL 动态壁纸","setting_wallpaper_path_hint_video":"视频文件路径（如 /sdcard/Download/wallpaper.mp4）","setting_wallpaper_path_hint_gl":"可选图片或视频路径（留空走程序化动画；视频如 /sdcard/wallpaper.mp4 走 GL 解码）","setting_wallpaper_paths_hint_image":"图片路径，多张用逗号分隔（如 /sdcard/a.jpg,/sdcard/b.jpg）","setting_carousel_interval_hint":"轮播间隔（毫秒，如 5000；仅图片模式）","setting_save":"保存","setting_cancel":"取消"},
    "en":    {"app_wallpaper":"Board Wallpaper","wallpaper_description":"Board Live Wallpaper","setting_wallpaper_path":"Wallpaper Path","setting_wallpaper_path_hint":"Enter wallpaper file path","setting_type_video":"Video Wallpaper","setting_type_image":"Image/Carousel Wallpaper","setting_type_gl":"OpenGL Live Wallpaper","setting_wallpaper_path_hint_video":"Video file path (e.g. /sdcard/Download/wallpaper.mp4)","setting_wallpaper_path_hint_gl":"Optional image or video path (leave empty for procedural animation; video e.g. /sdcard/wallpaper.mp4 uses GL decoding)","setting_wallpaper_paths_hint_image":"Image paths, separated by commas (e.g. /sdcard/a.jpg,/sdcard/b.jpg)","setting_carousel_interval_hint":"Carousel interval (ms, e.g. 5000; image mode only)","setting_save":"Save","setting_cancel":"Cancel"},
    "ru":    {"app_wallpaper":"Board Wallpaper","wallpaper_description":"Динамические обои Board","setting_wallpaper_path":"Путь к обоям","setting_wallpaper_path_hint":"Введите путь к файлу обоев","setting_type_video":"Видео обои","setting_type_image":"Изображения/Карусель обоев","setting_type_gl":"Динамические обои OpenGL","setting_wallpaper_path_hint_video":"Путь к видеофайлу (например, /sdcard/Download/wallpaper.mp4)","setting_wallpaper_path_hint_gl":"Путь к изображению или видео (оставьте пустым для процедурной анимации; видео, например, /sdcard/wallpaper.mp4 использует декодирование GL)","setting_wallpaper_paths_hint_image":"Пути к изображениям через запятую (например, /sdcard/a.jpg,/sdcard/b.jpg)","setting_carousel_interval_hint":"Интервал карусели (мс, например, 5000; только для режима изображений)","setting_save":"Сохранить","setting_cancel":"Отмена"},
    "pt":    {"app_wallpaper":"Board Wallpaper","wallpaper_description":"Papel de parede dinâmico Board","setting_wallpaper_path":"Caminho do papel de parede","setting_wallpaper_path_hint":"Insira o caminho do arquivo de papel de parede","setting_type_video":"Papel de parede de vídeo","setting_type_image":"Papel de parede de imagens/carrossel","setting_type_gl":"Papel de parede dinâmico OpenGL","setting_wallpaper_path_hint_video":"Caminho do arquivo de vídeo (ex: /sdcard/Download/wallpaper.mp4)","setting_wallpaper_path_hint_gl":"Caminho opcional de imagem ou vídeo (deixe em branco para animação procedural; vídeo ex: /sdcard/wallpaper.mp4 usa decodificação GL)","setting_wallpaper_paths_hint_image":"Caminhos das imagens, separados por vírgulas (ex: /sdcard/a.jpg,/sdcard/b.jpg)","setting_carousel_interval_hint":"Intervalo do carrossel (ms, ex: 5000; somente modo imagem)","setting_save":"Salvar","setting_cancel":"Cancelar"},
}

# ============ board/proxy ============
modules["board/proxy"] = {
    "zh-CN": {"app_name":"proxy"},
    "en":    {"app_name":"proxy"},
    "ru":    {"app_name":"proxy"},
    "pt":    {"app_name":"proxy"},
}

# ============ core/core-ui ============
modules["core/core-ui"] = {
    "zh-CN": {"srl_header_pulling":"下拉刷新","srl_header_refreshing":"正在刷新…","srl_header_loading":"正在加载…","srl_header_release":"释放刷新","srl_header_finish":"刷新完成","srl_header_failed":"刷新失败","srl_header_last_update":"上次更新 ","srl_header_update":"MM-dd HH:mm","srl_header_secondary":"进入二楼","srl_footer_pulling":"上拉加载","srl_footer_release":"释放加载","srl_footer_loading":"正在加载…","srl_footer_refreshing":"正在刷新…","srl_footer_finish":"加载完成","srl_footer_failed":"加载失败","srl_footer_nothing":"没有更多数据了"},
    "en":    {"srl_header_pulling":"Pull to refresh","srl_header_refreshing":"Refreshing...","srl_header_loading":"Loading...","srl_header_release":"Release to refresh","srl_header_finish":"Refresh complete","srl_header_failed":"Refresh failed","srl_header_last_update":"Last updated ","srl_header_update":"MM-dd HH:mm","srl_header_secondary":"Enter second floor","srl_footer_pulling":"Pull up to load","srl_footer_release":"Release to load","srl_footer_loading":"Loading...","srl_footer_refreshing":"Refreshing...","srl_footer_finish":"Load complete","srl_footer_failed":"Load failed","srl_footer_nothing":"No more data"},
    "ru":    {"srl_header_pulling":"Потяните для обновления","srl_header_refreshing":"Обновление...","srl_header_loading":"Загрузка...","srl_header_release":"Отпустите для обновления","srl_header_finish":"Обновление завершено","srl_header_failed":"Ошибка обновления","srl_header_last_update":"Последнее обновление ","srl_header_update":"MM-dd HH:mm","srl_header_secondary":"Войти на второй этаж","srl_footer_pulling":"Потяните вверх для загрузки","srl_footer_release":"Отпустите для загрузки","srl_footer_loading":"Загрузка...","srl_footer_refreshing":"Обновление...","srl_footer_finish":"Загрузка завершена","srl_footer_failed":"Ошибка загрузки","srl_footer_nothing":"Больше нет данных"},
    "pt":    {"srl_header_pulling":"Puxe para atualizar","srl_header_refreshing":"Atualizando...","srl_header_loading":"Carregando...","srl_header_release":"Solte para atualizar","srl_header_finish":"Atualização concluída","srl_header_failed":"Falha na atualização","srl_header_last_update":"Última atualização ","srl_header_update":"MM-dd HH:mm","srl_header_secondary":"Entrar no segundo andar","srl_footer_pulling":"Puxe para cima para carregar","srl_footer_release":"Solte para carregar","srl_footer_loading":"Carregando...","srl_footer_refreshing":"Atualizando...","srl_footer_finish":"Carregamento concluído","srl_footer_failed":"Falha no carregamento","srl_footer_nothing":"Sem mais dados"},
}

# ============ demo/task-demo ============
modules["demo/task-demo"] = {
    "zh-CN": {"app_name":"Task Demo","scene_basic":"基础用法","scene_state":"状态回调","scene_progress":"进度报告","scene_priority":"优先级队列","scene_concurrent":"并发下载","scene_lifecycle":"生命周期管理","start_task":"开始任务","cancel_task":"取消任务","cancel_all":"取消全部","clear_log":"清空日志","task_result":"任务结果: %s","task_error":"任务错误: %s","task_waiting":"等待中...","task_running":"执行中...","task_completed":"已完成","task_cancelled":"已取消"},
    "en":    {"app_name":"Task Demo","scene_basic":"Basic Usage","scene_state":"State Callback","scene_progress":"Progress Report","scene_priority":"Priority Queue","scene_concurrent":"Concurrent Download","scene_lifecycle":"Lifecycle Management","start_task":"Start Task","cancel_task":"Cancel Task","cancel_all":"Cancel All","clear_log":"Clear Log","task_result":"Task Result: %s","task_error":"Task Error: %s","task_waiting":"Waiting...","task_running":"Running...","task_completed":"Completed","task_cancelled":"Cancelled"},
    "ru":    {"app_name":"Task Demo","scene_basic":"Основное использование","scene_state":"Обратный вызов состояния","scene_progress":"Отчет о ходе выполнения","scene_priority":"Очередь приоритетов","scene_concurrent":"Параллельная загрузка","scene_lifecycle":"Управление жизненным циклом","start_task":"Начать задачу","cancel_task":"Отменить задачу","cancel_all":"Отменить все","clear_log":"Очистить журнал","task_result":"Результат задачи: %s","task_error":"Ошибка задачи: %s","task_waiting":"Ожидание...","task_running":"Выполнение...","task_completed":"Завершено","task_cancelled":"Отменено"},
    "pt":    {"app_name":"Task Demo","scene_basic":"Uso básico","scene_state":"Retorno de estado","scene_progress":"Relatório de progresso","scene_priority":"Fila de prioridade","scene_concurrent":"Download simultâneo","scene_lifecycle":"Gerenciamento de ciclo de vida","start_task":"Iniciar tarefa","cancel_task":"Cancelar tarefa","cancel_all":"Cancelar tudo","clear_log":"Limpar registro","task_result":"Resultado da tarefa: %s","task_error":"Erro da tarefa: %s","task_waiting":"Aguardando...","task_running":"Executando...","task_completed":"Concluído","task_cancelled":"Cancelado"},
}

# ============ demo/app-test ============
modules["demo/app-test"] = {
    "zh-CN": {"app_name":"app-test","app_test_resources_title":"测试资源"},
    "en":    {"app_name":"app-test","app_test_resources_title":"Test Resources"},
    "ru":    {"app_name":"app-test","app_test_resources_title":"Тестовые ресурсы"},
    "pt":    {"app_name":"app-test","app_test_resources_title":"Recursos de teste"},
}

# ============ demo/app_test_resources ============
modules["demo/app_test_resources"] = {
    "zh-CN": {"app_name":"测试资源","hello_world":"你好，世界！","resource_package":"资源包"},
    "en":    {"app_name":"Test Resources","hello_world":"Hello World!","resource_package":"Resource Package"},
    "ru":    {"app_name":"Test Resources","hello_world":"Привет, мир!","resource_package":"Пакет ресурсов"},
    "pt":    {"app_name":"Test Resources","hello_world":"Olá, mundo!","resource_package":"Pacote de recursos"},
}

# ============ demo/other-test ============
modules["demo/other-test"] = {
    "zh-CN": {"app_name":"other-test"},
    "en":    {"app_name":"other-test"},
    "ru":    {"app_name":"other-test"},
    "pt":    {"app_name":"other-test"},
}

# ============ demo/toSetting ============
modules["demo/toSetting"] = {
    "zh-CN": {"app_name":"ToSetting","icon_description":"设置图标"},
    "en":    {"app_name":"ToSetting","icon_description":"Settings Icon"},
    "ru":    {"app_name":"ToSetting","icon_description":"Значок настроек"},
    "pt":    {"app_name":"ToSetting","icon_description":"Ícone de configurações"},
}

# ============ Sample/remote_library_test_one ============
modules["Sample/remote_library_test_one"] = {
    "zh-CN": {"app_name":"Remote_Library_Test_One"},
    "en":    {"app_name":"Remote_Library_Test_One"},
    "ru":    {"app_name":"Remote_Library_Test_One"},
    "pt":    {"app_name":"Remote_Library_Test_One"},
}

# ============ Sample/remote_library_test_two ============
modules["Sample/remote_library_test_two"] = {
    "zh-CN": {"app_name":"Remote_Library_Test_two"},
    "en":    {"app_name":"Remote_Library_Test_two"},
    "ru":    {"app_name":"Remote_Library_Test_two"},
    "pt":    {"app_name":"Remote_Library_Test_two"},
}

# ============ 写入文件 ============
for module_dir, langs in modules.items():
    for lang_tag, strings in langs.items():
        filepath = os.path.join(BASE, module_dir, "translations", f"{lang_tag}.json")
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(strings, f, ensure_ascii=False, indent=2)
            f.write("\n")

print("全部 JSON 文件已生成！")
print(f"模块数: {len(modules)}")
print(f"文件总数: {sum(len(langs) for langs in modules.values())}")
