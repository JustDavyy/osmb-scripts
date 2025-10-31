import sys
import os
import requests

from PyQt5.QtWidgets import (
    QApplication, QWidget, QTabWidget, QVBoxLayout, QHBoxLayout,
    QListWidget, QListWidgetItem, QPushButton, QLabel,
    QLineEdit, QFileDialog, QProgressBar, QMessageBox, QTextEdit
)
from PyQt5.QtCore import Qt

USERS = [
    {
        "display": "Davyy",
        "github": "JustDavyy",
        "repo": "osmb-scripts",
        "branch": "main",
        "scripts": [
            {"name": "dAIOFisher", "template": "dAIOFisher/jar/dAIOFisher.jar"},
            {"name": "dAmethystMiner", "template": "dAmethystMiner/jar/dAmethystMiner.jar"},
            {"name": "dBattlestaffCrafter", "template": "dBattlestaffCrafter/jar/dBattlestaffCrafter.jar"},
            {"name": "dBoltEnchanter", "template": "dBoltEnchanter/jar/dBoltEnchanter.jar"},
            {"name": "dCamTorumMiner", "template": "dCamTorumMiner/jar/dCamTorumMiner.jar"},
            {"name": "dCannonballSmelter", "template": "dCannonballSmelter/jar/dCannonballSmelter.jar"},
            {"name": "dCastlewarsAFKer", "template": "dCastlewarsAFKer/jar/dCastlewarsAFKer.jar"},
            {"name": "dCooker", "template": "dCooker/jar/dCooker.jar"},
            {"name": "dFossilWCer", "template": "dFossilWCer/jar/dFossilWCer.jar"},
            {"name": "dGemstoneCrabber", "template": "dGemstoneCrabber/jar/dGemstoneCrabber.jar"},
            {"name": "dHarambeHunter", "template": "dHarambeHunter/jar/dHarambeHunter.jar"},
            {"name": "dLooter", "template": "dLooter/jar/dLooter.jar"},
            {"name": "dOffering", "template": "dOffering/jar/dOffering.jar"},
            {"name": "dPublicAlcher", "template": "dPublicAlcher/jar/dPublicAlcher.jar"},
            {"name": "dRangingGuild", "template": "dRangingGuild/jar/dRangingGuild.jar"},
            {"name": "dSawmillRunner", "template": "dSawmillRunner/jar/dSawmillRunner.jar"},
            {"name": "dTeleporter", "template": "dTeleporter/jar/dTeleporter.jar"},
            {"name": "dWinemaker", "template": "dWinemaker/jar/dWinemaker.jar"},
            {"name": "dWyrmAgility", "template": "dWyrmAgility/jar/dWyrmAgility.jar"},

            # Private scripts
            {"name": "dArceuusRCer", "template": "priv-jars/dArceuusRCer.jar"},
            {"name": "dERLooter", "template": "priv-jars/dERLooter.jar"},
            {"name": "dElfThiever", "template": "priv-jars/dElfThiever.jar"},
            {"name": "dWealthyCitizens", "template": "priv-jars/dWealthyCitizens.jar"},
            {"name": "dMahoganyHomes", "template": "priv-jars/dMahoganyHomes.jar"},
        ]
    },
    {
        "display": "Butter",
        "github": "ButterB21",
        "repo": "Butter-Scripts",
        "branch": "main",
        "scripts": [
            {"name": "Moths", "template": "Moths/jar/Moths.jar"},
            {"name": "OreBuyer", "template": "OreBuyer/jar/OreBuyer.jar"},

             # Private scripts
            {"name": "ButterFoundry", "template": "priv-jars/src_com_butterfoundry.jar"},
        ]
    },
    {
        "display": "Fru",
        "github": "fru-art",
        "repo": "fru-scripts",
        "branch": "master",
        "scripts": [
            {"name": "Camdozaal fisher", "template": "out/artifacts/CamdozaalFisherScript.jar"},
            {"name": "Camdozaal miner", "template": "out/artifacts/CamdozaalMinerScript.jar"},
            {"name": "Goblin slayer", "template": "out/artifacts/GoblinSlayerScript.jar"},
            {"name": "BeginnerPowerMinerScript", "template": "out/artifacts/BeginnerPowerMinerScript.jar"},
            {"name": "BeginnerWoodcutterScript", "template": "out/artifacts/BeginnerWoodcutterScript.jar"},
            {"name": "DarkWizardScript", "template": "out/artifacts/DarkWizardScript.jar"},
            {"name": "DumbTemporossScript", "template": "out/artifacts/DumbTemporossScript.jar"},
            {"name": "HerbCleanerScript", "template": "out/artifacts/HerbCleanerScript.jar"},
            {"name": "BeginnerPickpocketerScript", "template": "out/artifacts/BeginnerPickpocketerScript.jar"},
        ]
    },
    {
        "display": "jork",
        "scripts": [
            {"name": "BirdSnares", "url": "https://github.com/iamjakeirl/jorkScripts_free/blob/main/scripts/jorkHunter/jar/jorkHunter-BirdSnares.jar"},
            {"name": "Chinchompas", "url": "https://github.com/iamjakeirl/jorkScripts_free/blob/main/scripts/jorkHunter/jar/jorkHunter-Chinchompas.jar"},
            {"name": "jorkHunter", "url": "https://github.com/iamjakeirl/jorkScripts_free/blob/main/scripts/jorkHunter/jar/jorkHunter.jar"},
        ]
    },
    {
        "display": "SaMo",
        "github": "LazySaMo",
        "repo": "SaMo-community-scripts",
        "branch": "main",
        "scripts": [
            {"name": "BlisterwoodScript", "template": "BlisterwoodScript/jar/BlisterwoodScript.jar"},
            {"name": "DemonicTearsScript", "template": "DemonicTearsScript/jar/DemonicTearsScript.jar"},
            {"name": "VolcanicAshScript", "template": "VolcanicAshScript/jar/VolcanicAshScript.jar"}
        ]
    }
]

DEFAULT_DOWNLOAD_PATH = os.path.join(os.path.expanduser("~"), ".osmb", "Scripts")
if not os.path.exists(DEFAULT_DOWNLOAD_PATH):
    os.makedirs(DEFAULT_DOWNLOAD_PATH)

def get_download_url(user, script):
    if "url" in script:
        return script["url"]
    elif "template" in script:
        return f"https://raw.githubusercontent.com/{user.get('github','')}/{user.get('repo','')}/{user.get('branch','main')}/{script['template']}"
    else:
        return f"https://raw.githubusercontent.com/{user.get('github','')}/{user.get('repo','')}/{user.get('branch','main')}/{script['name']}/jar/{script['name']}.jar"

class ScriptTab(QWidget):
    def __init__(self, user):
        super().__init__()
        self.user = user
        self.layout = QVBoxLayout(self)
        self.filter_box = QLineEdit()
        self.filter_box.setPlaceholderText("Filter scripts...")
        self.layout.addWidget(self.filter_box)

        self.list_widget = QListWidget()
        for script in user["scripts"]:
            item = QListWidgetItem(script["name"])
            item.setFlags(item.flags() | Qt.ItemIsUserCheckable)
            item.setCheckState(Qt.Checked)
            self.list_widget.addItem(item)
        self.layout.addWidget(self.list_widget)

        btn_layout = QHBoxLayout()
        self.btn_all = QPushButton("Select All")
        self.btn_none = QPushButton("Deselect This List")
        btn_layout.addWidget(self.btn_all)
        btn_layout.addWidget(self.btn_none)
        self.layout.addLayout(btn_layout)

        self.filter_box.textChanged.connect(self.apply_filter)
        self.btn_all.clicked.connect(self.select_all)
        self.btn_none.clicked.connect(self.deselect_all)

    def apply_filter(self):
        filter_text = self.filter_box.text().lower()
        for i in range(self.list_widget.count()):
            item = self.list_widget.item(i)
            item.setHidden(filter_text not in item.text().lower())

    def select_all(self):
        for i in range(self.list_widget.count()):
            if not self.list_widget.item(i).isHidden():
                self.list_widget.item(i).setCheckState(Qt.Checked)

    def deselect_all(self):
        for i in range(self.list_widget.count()):
            if not self.list_widget.item(i).isHidden():
                self.list_widget.item(i).setCheckState(Qt.Unchecked)

    def get_selected_scripts(self):
        return [
            self.user["scripts"][i]
            for i in range(self.list_widget.count())
            if self.list_widget.item(i).checkState() == Qt.Checked
        ]

class MainWindow(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("OSMB Community Script Downloader by JS808 and JustDavyy v3.2")
        self.resize(660, 500)
        self.layout = QVBoxLayout(self)

        self.folder_label = QLabel("Download folder:")
        self.folder_box = QLineEdit(DEFAULT_DOWNLOAD_PATH)
        self.browse_btn = QPushButton("Browse...")
        folder_layout = QHBoxLayout()
        folder_layout.addWidget(self.folder_label)
        folder_layout.addWidget(self.folder_box)
        folder_layout.addWidget(self.browse_btn)
        self.layout.addLayout(folder_layout)
        self.browse_btn.clicked.connect(self.browse_folder)

        self.tabs = QTabWidget()
        self.tab_widgets = []
        for user in USERS:
            tab = ScriptTab(user)
            self.tabs.addTab(tab, user["display"])
            self.tab_widgets.append(tab)
        self.layout.addWidget(self.tabs)

        # Footer
        self.progress = QProgressBar()
        self.status_box = QTextEdit()
        self.status_box.setReadOnly(True)
        self.status_box.setMinimumHeight(100)
        self.status_box.setMaximumHeight(200)
        self.download_btn = QPushButton("Download Selected Scripts")
        self.open_folder_btn = QPushButton("Open Scripts Folder")
        self.deselect_all_btn = QPushButton("Deselect ALL Scripts")
        footer_layout = QHBoxLayout()
        footer_layout.addWidget(self.download_btn)
        footer_layout.addWidget(self.open_folder_btn)
        footer_layout.addWidget(self.deselect_all_btn)
        self.layout.addWidget(self.progress)
        self.layout.addWidget(self.status_box)
        self.layout.addLayout(footer_layout)
        self.download_btn.clicked.connect(self.download_selected)
        self.open_folder_btn.clicked.connect(self.open_folder)
        self.deselect_all_btn.clicked.connect(self.deselect_all_all_tabs)

    def browse_folder(self):
        path = QFileDialog.getExistingDirectory(self, "Choose download folder", self.folder_box.text())
        if path:
            self.folder_box.setText(path)

    def open_folder(self):
        folder = self.folder_box.text()
        if not os.path.exists(folder):
            QMessageBox.warning(self, "Error", "Folder does not exist yet.")
            return
        os.startfile(folder)

    def deselect_all_all_tabs(self):
        for tab in self.tab_widgets:
            for i in range(tab.list_widget.count()):
                tab.list_widget.item(i).setCheckState(Qt.Unchecked)

    def download_selected(self):
        folder = self.folder_box.text()
        if not os.path.exists(folder):
            os.makedirs(folder)
        selected = []
        for tab in self.tab_widgets:
            selected += [(tab.user, script) for script in tab.get_selected_scripts()]
        if not selected:
            QMessageBox.warning(self, "Error", "Please select at least one script.")
            return
        self.progress.setMaximum(len(selected))
        self.progress.setValue(0)
        self.status_box.clear()
        for idx, (user, script) in enumerate(selected, 1):
            url = get_download_url(user, script)
            local_path = os.path.join(folder, f"{script['name']}.jar")
            try:
                r = requests.get(url, allow_redirects=True, timeout=20)
                r.raise_for_status()
                with open(local_path, "wb") as f:
                    f.write(r.content)
                self.status_box.append(f"UPDATED: {user['display']} - {script['name']}")
            except Exception as ex:
                self.status_box.append(f"FAILED: {user['display']} - {script['name']} -> {url}")
            self.progress.setValue(idx)
            self.status_box.moveCursor(self.status_box.textCursor().End)
        self.status_box.append("All done!")
        self.status_box.moveCursor(self.status_box.textCursor().End)

# --- DARK PALETTE FUNCTION ---
def apply_dark_palette(app):
    from PyQt5.QtGui import QPalette, QColor
    dark_palette = QPalette()
    dark_palette.setColor(QPalette.Window, QColor(26, 26, 30))
    dark_palette.setColor(QPalette.WindowText, Qt.white)
    dark_palette.setColor(QPalette.Base, QColor(40, 40, 44))
    dark_palette.setColor(QPalette.AlternateBase, QColor(30, 30, 34))
    dark_palette.setColor(QPalette.Text, Qt.white)
    dark_palette.setColor(QPalette.Button, QColor(45, 45, 50))
    dark_palette.setColor(QPalette.ButtonText, Qt.white)
    dark_palette.setColor(QPalette.BrightText, Qt.red)
    dark_palette.setColor(QPalette.Highlight, QColor(70,130,180))
    dark_palette.setColor(QPalette.HighlightedText, Qt.black)
    app.setPalette(dark_palette)

if __name__ == "__main__":
    app = QApplication(sys.argv)
    apply_dark_palette(app)
    app.setStyleSheet("""
    QWidget {
        background-color: #1a1a1e;
        color: #fff;
        font: 10pt Segoe UI, Arial, sans-serif;
    }
    QLineEdit, QTextEdit {
        background: #222228;
        color: #fff;
        border: 1px solid #444;
        padding: 3px;
    }
    QTabBar::tab {
        background: #333 !important;
        color: #bbb !important;
        padding: 6px 12px;
        font-weight: bold;
        border: 1px solid #222;
        border-bottom: none;
        min-width: 70px;
    }
    QTabBar::tab:selected {
        background: #222 !important;
        color: #fff !important;
        border: 1px solid #666 !important;
        border-bottom: 2px solid #7bae49 !important;
    }
    QTabWidget::pane {
        border: 1px solid #222;
        background: #25252a;
        margin: 0;
    }
    QListWidget, QPlainTextEdit {
        background: #18181d;
        color: #e6e6e6;
        selection-background-color: #292932;
        border: 1px solid #353535;
    }
    QCheckBox {
        spacing: 8px;
    }
    QPushButton {
        background-color: #32323a;
        color: #fff;
        border: 1px solid #444;
        padding: 6px 12px;
        border-radius: 3px;
    }
    QPushButton:disabled {
        background-color: #222;
        color: #555;
        border: 1px solid #222;
    }
    QLabel {
        color: #d9d9d9;
    }
    QProgressBar {
        background: #232328;
        color: #fff;
        border: 1px solid #444;
        border-radius: 3px;
        height: 12px;
        text-align: center;
    }
    QProgressBar::chunk {
        background: #7bae49;
        border-radius: 3px;
    }
""")
    win = MainWindow()
    win.show()
    sys.exit(app.exec_())
