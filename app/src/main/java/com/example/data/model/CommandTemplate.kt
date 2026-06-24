package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class CommandTemplate(
    val id: String,
    val title: String,
    val shortDescription: String,
    val fullExplanation: String,
    val promptDraft: String,
    val category: String
)

object CommandTemplateLibrary {
    val templates = listOf(
        CommandTemplate(
            id = "sys_update",
            title = "System Update Prep",
            shortDescription = "Refresh repo caches and check updates",
            fullExplanation = "Queries system package managers (apt, dnf, pacman) to safely fetch package indices and list pending upgrades without installing them.",
            promptDraft = "Fetch update repository listings and identify package security upgrades available on the machine.",
            category = "System"
        ),
        CommandTemplate(
            id = "ssh_audit",
            title = "SSH Security Audit",
            shortDescription = "Inspect recent login activities",
            fullExplanation = "Scans authentication logs (auth.log/secure) to detect failed login attempts, list suspicious client IPs, and report successful handshakes.",
            promptDraft = "Examine authentication logs to report failed login attempts, list active user sessions, and summarize suspicious SSH activity.",
            category = "Security"
        ),
        CommandTemplate(
            id = "disk_usage",
            title = "Disk Space Analyzer",
            shortDescription = "Find directory sizing and partitions",
            fullExplanation = "Analyzes high-capacity directories and partitions, running safe, optimized find/du commands to pinpoint large storage consumers.",
            promptDraft = "Identify the top 5 largest directories under system storage and list active disk partitions exceeding 80% usage.",
            category = "Storage"
        ),
        CommandTemplate(
            id = "service_health",
            title = "Service Status Audit",
            shortDescription = "Analyze running & failed services",
            fullExplanation = "Inspects systemd or daemon processes to guarantee essential server-level daemons are running correctly and list any crashed units.",
            promptDraft = "Check status of systemd services, list any failed units, and confirm core daemons like sshd or nginx are operational.",
            category = "Health"
        ),
        CommandTemplate(
            id = "system_resources",
            title = "Sys Resource Load",
            shortDescription = "Inspect CPU/RAM stress levels",
            fullExplanation = "Examines CPU load averages, physical RAM depletion, and identifies the top 5 resource-hungry active tasks.",
            promptDraft = "Display live system load averages, detailed RAM swap details, and highlight the top 5 CPU/Memory consuming processes.",
            category = "Performance"
        ),
        CommandTemplate(
            id = "docker_audit",
            title = "Docker Status Audit",
            shortDescription = "Audit active containers & footprint",
            fullExplanation = "Leverages dockerd statistics and list commands to check container health, resource utilization, and locate dangling image assets.",
            promptDraft = "Inspect all running and stopped Docker containers, report their memory footprints, and check for any container restart issues.",
            category = "Docker"
        ),
        CommandTemplate(
            id = "firewall_check",
            title = "Firewall & Ports Audit",
            shortDescription = "Verify active connections and ports",
            fullExplanation = "Audits network interfaces, open TCP/UDP listening ports, and lists current firewall configurations (ufw, iptables) for security.",
            promptDraft = "Scan active listening ports, list current active connections, and summarize active firewall security rules.",
            category = "Security"
        )
    )
}
