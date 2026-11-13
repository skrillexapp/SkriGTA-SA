#pragma once

#include <string>

struct Glory
{
    std::string szHost = "10.25.60.18";
    int iPort = 7777;
};

inline Glory GetGlory()
{
    Glory config;
    return config;
}
